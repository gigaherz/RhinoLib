package dev.gigaherz.rhinolib;

public class IdFunctionObjectES6 extends IdFunctionObject {
	private static final int Id_length = 1;
	private static final int Id_name = 3;
	private boolean myLength = true;
	private boolean myName = true;

	public IdFunctionObjectES6(IdFunctionCall idcall, Object tag, int id, String name, int arity, Scriptable scope) {
		super(idcall, tag, id, name, arity, scope);
	}

	@Override
	protected int findInstanceIdInfo(String s, Context cx) {
		if (s.equals("length")) {
			return instanceIdInfo(READONLY | DONTENUM, Id_length);
		} else if (s.equals("name")) {
			return instanceIdInfo(READONLY | DONTENUM, Id_name);
		}
		return super.findInstanceIdInfo(s, cx);
	}

	@Override
	protected Object getInstanceIdValue(int id, Context cx) {
		if (id == Id_length && !myLength) {
			return NOT_FOUND;
		} else if (id == Id_name && !myName) {
			return NOT_FOUND;
		}
		return super.getInstanceIdValue(id, cx);
	}

	@Override
	protected void setInstanceIdValue(int id, Object value, Context cx) {
		if (id == Id_length && value == NOT_FOUND) {
			this.myLength = false;
			return;
		} else if (id == Id_name && value == NOT_FOUND) {
			this.myName = false;
			return;
		}
		super.setInstanceIdValue(id, value, cx);
	}
}
