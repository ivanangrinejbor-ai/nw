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

/**
 * XStream converter for {@code short[]} (tilemap layer data).
 *
 * <p>Stores the array as a Base64-encoded, big-endian byte blob:
 * <pre>{@code <layer enc="b64">AAACAAD/...==</layer>}</pre>
 * This avoids XStream's default {@code <short-array>} behaviour which writes
 * thousands of {@code <short>} child elements for a 16×12 map (2304 elements)
 * and causes {@link com.thoughtworks.xstream.converters.ConversionException} on
 * deserialization of primitive arrays registered under a security-restricted
 * XStream instance (the situation in Catroid's {@link XstreamSerializer}).
 *
 * <p><b>Format:</b> 2 bytes per cell, big-endian (network order), Base64
 * {@link Base64#NO_WRAP}. Empty array ({@code short[0]}) is encoded as {@code ""}.
 *
 * <p>Backward compat: if the element has no {@code enc="b64"} attribute the
 * converter falls back to reading space-separated decimal shorts, so any
 * hand-authored XML or older format still loads.
 */
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
        // 2 bytes per short, big-endian
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
            // Modern format: Base64-encoded big-endian bytes
            byte[] bytes = Base64.decode(value, Base64.NO_WRAP);
            if (bytes.length % 2 != 0) {
                // Corrupted — return empty rather than crash
                return new short[0];
            }
            ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            short[] arr = new short[bytes.length / 2];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = buf.getShort();
            }
            return arr;
        } else {
            // Legacy fallback: space-separated decimal integers
            String[] tokens = value.trim().split("\\s+");
            short[] arr = new short[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                try {
                    arr[i] = Short.parseShort(tokens[i]);
                } catch (NumberFormatException ignored) {
                    arr[i] = -1; // EMPTY
                }
            }
            return arr;
        }
    }
}
