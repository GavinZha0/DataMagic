package com.ninestar.datapie.framework.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TreeUtils {

    //fields of id and pid
    static String pidType = "int";
    static Field pidField = null;
    static Field idField = null;
    static Field childField = null;

    //Root node value
    static String rootId = "0";

    //supported types of pid
    static String stringType = "class java.lang.String";
    static String longType = "class java.lang.Long";
    static String integerType = "class java.lang.Integer";
    static String intType = "int";

    //Primary key id
    static String id = "id";

    //Parent node id
    static String pid = "pid";

    //Child node collection
    static String child = "children";

    /**
     * Build tree from list
     * @param planeList t
     * @param <T> t
     * @return result
     * @throws IllegalAccessException result
     */
    public static <T> List<T> buildTree(List<T> planeList, String idName, String pidName, String childName) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        if(planeList==null || planeList.size()==0){
            return null;
        }

        // get field names
        id = idName;
        pid = pidName;
        child = childName;

        // get field definitions
        idField = getField(planeList.get(0), id);
        pidField = getField(planeList.get(0), pid);
        childField = getField(planeList.get(0), child);
        pidType = pidField.getType().toString();

        // root node collection
        List<T> rootList = new ArrayList<T>();
        HashMap<Object, List<T>> pidTrees= new HashMap<>();

        // get root nodes
        for (final T t : planeList) {

            Object value = getPid(t);
            if(value!=null){
                if(pidTrees.get(value)!=null) {
                    pidTrees.get(value).add(t);
                }else{
                    pidTrees.put(value,new ArrayList<T>(){{this.add(t);}});
                }

                //add to rootList if it is a root node
                //support multiple types
                if (stringType.equals(pidType) && rootId.equals(value)) {
                    rootList.add(t);
                } else if (longType.equals(pidType) && ((Long)Long.parseLong(rootId)).equals(value)) {
                    rootList.add(t);
                } else if (integerType.equals(pidType) && ((Integer)Integer.parseInt(rootId)).equals(value)) {
                    rootList.add(t);
                } else if (intType.equals(pidType) && ((Integer)Integer.parseInt(rootId)).equals(value)) {
                    rootList.add(t);
                }
            }
            else{
                //it is root node if pid is null
                rootList.add(t);
            }
        }
        buildChildren(rootList,pidTrees);
        return rootList;
    }

    private static   <T>  Field getField(T t, String key) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        Field[] fields = t.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if(field.getName().equals(key)){
                return field;
            }
        }
        return null;
    }

    private static   <T>  Object getId(T t) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        return idField.get(t);
    }
    private static   <T>  Object getPid(T t) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        return pidField.get(t);
    }
    private static   <T>  Object getChild(T t) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        return childField.get(t);
    }
    private static   <T>  Object setChild(T t, Object value) throws IllegalAccessException {
        childField.set(t,value);
        return null;
    }
    /**
     * Get field value
     * @param t
     * @param key
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */
    private static   <T>  Object getValue(T t,String key) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        Class tClass = Class.forName(t.getClass().getName());
        Field tField = tClass.getField(key);
        Object tValue = tField.get(t);
        return tValue;
    }
    /**
     * Set field value
     * @param t
     * @param key
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */
    private static   <T>  Object setValue(T t,String key, Object value) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        Class tClass = Class.forName(t.getClass().getName());
        Field tField = tClass.getField(key);
        tField.set(t, value);
        return null;
    }
    /**
     * Get field type
     * @param t
     * @param key
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */
    public static   <T>  String getType(T t,String key) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        Class tClass = Class.forName(t.getClass().getName());
        Field tField = tClass.getField(key);
        return tField.getType().toString();
    }

    /**
     * Recursive construction
     * @param currentTrees
     * @param pidTrees
     * @param <T>
     * @throws IllegalAccessException
     */
    private static  <T> void  buildChildren(List<T> currentTrees, HashMap<Object, List<T>>  pidTrees) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        for (T t : currentTrees) {
            Object nodeId = getId(t);
            //Data exists with current id as pid
            if(pidTrees.get(nodeId)!=null){
                //add children to current tree
                List list = (List) getChild(t);
                list.addAll(pidTrees.get(nodeId));

                //add children's children recursively
                buildChildren(pidTrees.get(nodeId),pidTrees);
            }else {
                //leaf node doesn't have child
                setChild(t, null);
            }
        }
    }
}
