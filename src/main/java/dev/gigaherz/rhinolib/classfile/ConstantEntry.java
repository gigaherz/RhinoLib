/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.classfile;

final class ConstantEntry {
	private final int type;
	private final int intval;
	private final String str1;
	private final String str2;
	private final int hashcode;
	private long longval;

	ConstantEntry(int type, int intval, String str1, String str2) {
		this.type = type;
		this.intval = intval;
		this.str1 = str1;
		this.str2 = str2;
		hashcode = type ^ intval + str1.hashCode() * str2.hashCode();
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConstantEntry entry)) {
			return false;
		}
		if (type != entry.type) {
			return false;
		}
		return switch (type) {
			case ConstantPool.CONSTANT_Integer, ConstantPool.CONSTANT_Float -> intval == entry.intval;
			case ConstantPool.CONSTANT_Long, ConstantPool.CONSTANT_Double -> longval == entry.longval;
			case ConstantPool.CONSTANT_NameAndType -> str1.equals(entry.str1) && str2.equals(entry.str2);
			case ConstantPool.CONSTANT_InvokeDynamic -> intval == entry.intval && str1.equals(entry.str1) && str2.equals(entry.str2);
			default -> throw new RuntimeException("unsupported constant type");
		};
	}
}
