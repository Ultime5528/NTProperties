package com.ultime5528.ntproperties;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class NTProperties {

    public final static String TABLE_NAME = "NTProperties";
    private static final Set<Class<?>> SUPPORTED_TYPES;

    static {
        Set<Class<?>> supportedTypes = new HashSet<>(3);
        supportedTypes.add(double.class);
        supportedTypes.add(int.class);
        supportedTypes.add(boolean.class);
        SUPPORTED_TYPES = Collections.unmodifiableSet(supportedTypes);
    }

    private NetworkTableInstance ntInst;

    private Queue<Change> changes;
    private Object lock;

    private ArrayList<Entry> entries;

    public NTProperties(Class<?> propertyClass) {

        changes = new LinkedList<>();
        lock = new Object();

        entries = new ArrayList<>();

        ntInst = NetworkTableInstance.getDefault();

        addTable(propertyClass, ntInst.getTable(TABLE_NAME));

    }

    /**
     * Updates the Properties Class synchronously on the main robot thread.
     * This should be called periodically, for example, in <code>Robot.periodic()</code>.
     */
    public void performChanges() {

        synchronized(lock) {

            while(!changes.isEmpty()) {
                Change change = changes.remove();
                change.entry.performChange(change.notif);
            }

        }

    }

    // Recursive
    private void addTable(Class<?> root, NetworkTable table) {
        
        addFields(root, table);

        Class<?>[] innerClasses = root.getClasses();

        for(Class<?> theClass : innerClasses) {
            if (Modifier.isStatic(theClass.getModifiers()))
                addTable(theClass, table.getSubTable(theClass.getSimpleName()));
        }

    }

    private void addFields(Class<?> theClass, NetworkTable table) {

        for (Field f : theClass.getFields()) {
            
            int fieldModifier = f.getModifiers();
            Class<?> type = f.getType();

            // Field must be : non-final, static and supported (double, int or boolean)
            if (!Modifier.isFinal(fieldModifier) && Modifier.isStatic(fieldModifier)
                    && (SUPPORTED_TYPES.contains(type))) { 

                entries.add(new Entry(f, table));

            }
        }

    }


    /**
     * A Entry is a field that was added to a NetworkTable.
     */
    private class Entry {

        NetworkTableEntry entry;
        Field field;
        Class<?> type;
        Class<?> declaringClass;
        Method callbackMethod;

        /**
         * Adds a Field to a NetworkTable and registers the corresponding callbacks and listeners.
         * @param field
         * @param table
         */
        public Entry(Field field, NetworkTable table) {

            this.field = field;
            entry = table.getEntry(field.getName());
            type = field.getType();
            declaringClass = field.getDeclaringClass();
            
            setupCallbackIfPresent();

            try {

                if (type == double.class) {
                    entry.setDouble(field.getDouble(null));
                } else if (type == int.class) {
                    entry.setDouble(field.getInt(null));
                } else if(type == boolean.class) {
                    entry.setBoolean(field.getBoolean(null));
                } else {
                    
                    String message = String.format("Type of field \"%s\" inside class \"%s\" is %s, which is not supported",
                            field.getName(), declaringClass.getName(), type.getName());

                    throw new NTPropertiesException(message);
                }

            } catch (IllegalAccessException e) {
                String message = String.format("Field \"%s\" inside class \"%s\" is not accessible",
                            field.getName(), declaringClass.getName());

                throw new NTPropertiesException(message, e);
            }

            entry.addListener(this::handleNotification, EntryListenerFlags.kUpdate);

        }

        private void setupCallbackIfPresent() {

            Callback callbackAnnot = field.getAnnotation(Callback.class);

            if(callbackAnnot != null) {
                
                String callbackName = callbackAnnot.value();

                // Provided callback name cannot be null or empty
                if(callbackName == null || callbackName.isEmpty()) {

                    throw new NTPropertiesException("The callback \"" +
                        callbackName + "\" of field \"" +
                        field.getName() + "\" inside class \"" +
                        declaringClass.getName() + "\" cannot be null or empty.");

                }

                try {

                    callbackMethod = declaringClass.getMethod(callbackName);

                } catch (NoSuchMethodException e) {

                    throw new NTPropertiesException("The callback \"" + 
                        callbackName + "\" of field \"" +
                        field.getName() + "\" is not declared inside class \"" +
                        declaringClass.getName() + "\".", e);

                } catch (SecurityException e) {

                    throw new NTPropertiesException("The callback \"" +
                        callbackName + "\" of field \"" +
                        field.getName() + "\" inside class \"" +
                        declaringClass.getName() + "\" is not accessible.");

                }

            }

        }

        /**
         * Handles the notification received from the NetworkTables.
         * @param notif
         */
        private void handleNotification(EntryNotification notif) {

            synchronized (lock) {
                changes.add(new Change(this, notif));
            }

        }


        /**
         * Performs the change associated with the notification.
         * @param notif the received notification
         */
        private void performChange(EntryNotification notif) {

            try {

                if (type == double.class) {
                    field.set(null, notif.value.getDouble());
                } else if(type == int.class) {
                    field.set(null, (int) notif.value.getDouble());
                } else if(type == boolean.class) {
                    field.set(null, notif.value.getBoolean());
                }

                if (callbackMethod != null) {
                    try {
                        callbackMethod.invoke(null);
                    } catch (IllegalArgumentException | InvocationTargetException e) {
                        throw new NTPropertiesException("Callback method \"" + callbackMethod.getName()
                            + "\" cannot be called", e);
                    } 
                }

            } catch (IllegalAccessException e) {
                throw new NTPropertiesException("Cannot access field " + field.getName(), e);
            }

        }

    }


    /**
     * A Change that was made in the NetworkTable
     */
    private static class Change {

        public final EntryNotification notif;
        public final Entry entry;

        /**
         * 
         * @param entry the Entry in the NTProperties class
         * @param notif the EntryNotification that was received.
         */
        public Change(Entry entry, EntryNotification notif) {
            this.entry = entry;
            this.notif = notif;
        }

    }

}