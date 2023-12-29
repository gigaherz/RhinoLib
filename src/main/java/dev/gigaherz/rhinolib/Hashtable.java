package dev.gigaherz.rhinolib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This generic hash table class is used by Set and Map. It uses
 * a standard HashMap for storing keys and values so that we can handle
 * lots of hash collisions if necessary, and a doubly-linked list to support the iterator
 * capability.
 * <p>
 * This second one is important because JavaScript handling of
 * the iterator is completely different from the way that Java does it. In Java
 * an attempt to modify a collection on a HashMap or LinkedHashMap while iterating
 * through it (except by using the "remove" method on the Iterator object itself) results in a
 * ConcurrentModificationException. JavaScript Maps and Sets explicitly allow
 * the collection to be modified, or even cleared completely, while iterators
 * exist, and even lets an iterator keep on iterating on a collection that was
 * empty when it was created..
 */
public class Hashtable implements Iterable<Hashtable.Entry> {
	/**
	 * One entry in the hash table. Override equals and hashcode because this is
	 * another area in which JavaScript and Java differ. This entry also becomes a
	 * node in the linked list.
	 */

	public static final class Entry {
		private final Context localContext;
		private final int hashCode;
		Object key;
		Object value;
		private boolean deleted;
		private Entry next;
		private Entry prev;

		Entry(Context cx) {
			localContext = cx;
			hashCode = 0;
		}

		Entry(Context cx, Object k, Object value) {
			localContext = cx;

			if ((k instanceof Number) && (!(k instanceof Double))) {
				// Hash comparison won't work if we don't do this
				this.key = ((Number) k).doubleValue();
			} else if (k instanceof ConsString) {
				this.key = k.toString();
			} else {
				this.key = k;
			}

			if (key == null) {
				hashCode = 0;
			} else if (k.equals(ScriptRuntime.negativeZeroObj)) {
				hashCode = 0;
			} else {
				hashCode = key.hashCode();
			}

			this.value = value;
		}

		public Object key() {
			return key;
		}

		public Object value() {
			return value;
		}

		/**
		 * Zero out key and value and return old value.
		 */
		Object clear() {
			final Object ret = value;
			key = Undefined.instance;
			value = Undefined.instance;
			deleted = true;
			return ret;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			try {
				return ScriptRuntime.sameZero(localContext, key, ((Entry) o).key);
			} catch (ClassCastException cce) {
				return false;
			}
		}
	}

	// The iterator for this class works directly on the linked list so that it implements
	// the specified iteration behavior, which is very different from Java.
	private final static class Iter implements Iterator<Entry> {
		private Entry pos;

		Iter(Context cx, Entry start) {
			// Keep the logic simpler by having a dummy at the start
			Entry dummy = makeDummy(cx);
			dummy.next = start;
			this.pos = dummy;
		}

		private void skipDeleted() {
			// Skip forward past deleted elements, which could appear due to
			// "delete" or a "clear" operation after this iterator was created.
			// End up just before the next non-deleted node.
			while ((pos.next != null) && pos.next.deleted) {
				pos = pos.next;
			}
		}

		@Override
		public boolean hasNext() {
			skipDeleted();
			return ((pos != null) && (pos.next != null));
		}

		@Override
		public Entry next() {
			skipDeleted();
			if ((pos == null) || (pos.next == null)) {
				throw new NoSuchElementException();
			}
			final Entry e = pos.next;
			pos = pos.next;
			return e;
		}
	}

	private static Entry makeDummy(Context cx) {
		final Entry d = new Entry(cx);
		d.clear();
		return d;
	}

	private final Context localContext;
	private final HashMap<Object, Entry> map = new HashMap<>();
	private Entry first = null;
	private Entry last = null;

	public Hashtable(Context cx) {
		localContext = cx;
	}

	public int size() {
		return map.size();
	}

	public void put(Context cx, Object key, Object value) {
		final Entry nv = new Entry(cx, key, value);
		final Entry ev = map.putIfAbsent(nv, nv);
		if (ev == null) {
			// New value -- insert to end of doubly-linked list
			if (first == null) {
				first = last = nv;
			} else {
				last.next = nv;
				nv.prev = last;
				last = nv;
			}
		} else {
			// Update the existing value and keep it in the same place in the list
			ev.value = value;
		}
	}

	public Object get(Context cx, Object key) {
		final Entry e = new Entry(cx, key, null);
		final Entry v = map.get(e);
		if (v == null) {
			return null;
		}
		return v.value;
	}

	public boolean has(Context cx, Object key) {
		final Entry e = new Entry(cx, key, null);
		return map.containsKey(e);
	}

	public Object delete(Context cx, Object key) {
		final Entry e = new Entry(cx, key, null);
		final Entry v = map.remove(e);
		if (v == null) {
			return null;
		}

		// To keep existing iterators moving forward as specified in EC262,
		// we will remove the "prev" pointers from the list but leave the "next"
		// pointers intact. Once we do that, then the only things pointing to
		// the deleted nodes are existing iterators. Once those are gone, then
		// these objects will be GCed.
		// This way, new iterators will not "see" the deleted elements, and
		// existing iterators will continue from wherever they left off to
		// continue iterating in insertion order.
		if (v == first) {
			if (v == last) {
				// Removing the only element. Leave it as a dummy or existing iterators
				// will never stop.
				v.clear();
				v.prev = null;
			} else {
				first = v.next;
				first.prev = null;
				if (first.next != null) {
					first.next.prev = first;
				}
			}
		} else {
			final Entry prev = v.prev;
			prev.next = v.next;
			v.prev = null;
			if (v.next != null) {
				v.next.prev = prev;
			} else {
				assert (v == last);
				last = prev;
			}
		}
		// Still clear the node in case it is in the chain of some iterator
		return v.clear();
	}

	public void clear(Context cx) {
		// Zero out all the entries so that existing iterators will skip them all
		Iterator<Entry> it = iterator();
		it.forEachRemaining(Entry::clear);

		// Replace the existing list with a dummy, and make it the last node
		// of the current list. If new nodes are added now, existing iterators
		// will drive forward right into the new list. If they are not, then
		// nothing is referencing the old list and it'll get GCed.
		if (first != null) {
			Entry dummy = makeDummy(cx);
			last.next = dummy;
			first = last = dummy;
		}

		// Now we can clear the actual hashtable!
		map.clear();
	}

	@Override
	public Iterator<Entry> iterator() {
		return new Iter(localContext, first);
	}
}
