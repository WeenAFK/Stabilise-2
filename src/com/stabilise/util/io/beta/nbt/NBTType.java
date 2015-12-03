package com.stabilise.util.io.beta.nbt;

import java.util.function.Supplier;

import com.stabilise.util.box.ByteArrBox;
import com.stabilise.util.box.ByteBox;
import com.stabilise.util.box.DoubleBox;
import com.stabilise.util.box.FloatBox;
import com.stabilise.util.box.IntArrBox;
import com.stabilise.util.box.IntBox;
import com.stabilise.util.box.LongBox;
import com.stabilise.util.box.ShortBox;
import com.stabilise.util.box.StringBox;
import com.stabilise.util.collect.registry.Registries;
import com.stabilise.util.collect.registry.TypeFactory;
import com.stabilise.util.io.Sendable;


public enum NBTType {
    
    BYTE      (1,  ByteBox.class),
    SHORT     (2,  ShortBox.class),
    INT       (3,  IntBox.class),
    LONG      (4,  LongBox.class),
    FLOAT     (5,  FloatBox.class),
    DOUBLE    (6,  DoubleBox.class),
    BYTE_ARRAY(7,  ByteArrBox.class),
    STING     (8,  StringBox.class),
    LIST      (9,  NBTList.class, NBTList::new),
    COMPOUND  (10, NBTCompound.class, NBTCompound::new),
    INT_ARRAY (11, IntArrBox.class);
    
    
    public final byte id;
    private final Class<? extends Sendable> type;
    private final Supplier<Sendable> fac;
    
    private NBTType(int id, Class<? extends Sendable> type) {
        this(id, type, null);
    }
    
    private NBTType(int id, Class<? extends Sendable> type, Supplier<Sendable> fac) {
        this.id = (byte)id;
        this.type = type;
        this.fac = fac;
    }
    
    private static final TypeFactory<Sendable> registry = Registries.typeFactory();
    
    static {
        for(NBTType t : NBTType.values()) {
            if(t.fac == null)
                registry.registerUnsafe(t.id, t.type);
            else
                registry.register(t.id, t.type, t.fac);
        }
            
    }
    
    public static Sendable createTag(byte id) {
        return registry.create(id);
    }
    
    public static byte tagID(Sendable s) {
        return (byte) registry.getID(s.getClass());
    }
    
    public static String name(byte id) {
        return NBTType.values()[id-1].toString();
    }
    
}
