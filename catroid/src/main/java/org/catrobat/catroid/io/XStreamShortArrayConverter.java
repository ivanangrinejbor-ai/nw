/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.io;

import android.util.Base64;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class XStreamShortArrayConverter implements Converter {

    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
        return short[].class.equals(type);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        short[] arr = (short[]) source;
        if (arr == null || arr.length == 0) {
            writer.addAttribute("enc", "b64");
            writer.setValue("");
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(arr.length * 2).order(ByteOrder.BIG_ENDIAN);
        for (short s : arr) {
            buf.putShort(s);
        }
        writer.addAttribute("enc", "b64");
        writer.setValue(Base64.encodeToString(buf.array(), Base64.NO_WRAP));
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String enc = reader.getAttribute("enc");
        String value = reader.getValue();
        if (value == null || value.isEmpty()) {
            return new short[0];
        }
        if ("b64".equals(enc)) {
            byte[] bytes = Base64.decode(value, Base64.NO_WRAP);
            if (bytes.length % 2 != 0) {
                return new short[0];
            }
            ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            short[] arr = new short[bytes.length / 2];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = buf.getShort();
            }
            return arr;
        } else {
            String[] tokens = value.trim().split("\\s+");
            short[] arr = new short[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                try {
                    arr[i] = Short.parseShort(tokens[i]);
                } catch (NumberFormatException ignored) {
                    arr[i] = -1;
                }
            }
            return arr;
        }
    }
}
