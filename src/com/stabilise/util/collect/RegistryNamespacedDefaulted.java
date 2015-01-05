package com.stabilise.util.collect;

/**
 * This class allows for the creation of a namespaced registry which returns a
 * specified default value if no other value could be found.
 * 
 * <p>This class has been reconstructed from the decompiled Minecraft 1.7.10
 * source.
 */
public class RegistryNamespacedDefaulted<V> extends RegistryNamespaced<V> {
	
	/** The name mapped to the default value. */
	private final String defaultName;
	/** The default value. */
	private V defaultValue = null;
	
	
	/**
	 * Creates a new namespaced registry with an initial capacity of 16.
	 * Attempting to register duplicate names and IDs in this registry will
	 * result in the newer ones being ignored.
	 * 
	 * @param name The name of the registry.
	 * @param defaultNamespace The default namespace under which to register
	 * objects.
	 * @param defaultName The name under which the default return value will be
	 * registered.
	 * 
	 * @throws NullPointerException if either {@code name}, {@code
	 * defaultNamespace}, or {@code defaultName} are {@code null}.
	 */
	public RegistryNamespacedDefaulted(String name, String defaultNamespace, String defaultName) {
		this(name, defaultNamespace, defaultName, 16);
	}
	
	/**
	 * Creates a new namespaced registry. Attempting to register duplicate
	 * names and IDs in this registry will result in the new ones being
	 * ignored.
	 * 
	 * @param name The name of the registry.
	 * @param defaultNamespace The default namespace under which to register
	 * objects.
	 * @param defaultName The name under which the default return value will be
	 * registered.
	 * @param capacity The initial registry capacity.
	 * 
	 * @throws NullPointerException if either {@code name}, {@code
	 * defaultNamespace}, or {@code defaultName} are {@code null}.
	 * @throws IllegalArgumentException if {@code capacity < 0}.
	 */
	public RegistryNamespacedDefaulted(String name, String defaultNamespace, String defaultName,
			int capacity) {
		this(name, defaultNamespace, defaultName, capacity, false);
	}
	
	/**
	 * Creates a new namespaced registry.
	 * 
	 * @param name The name of the registry.
	 * @param defaultNamespace The default namespace under which to register
	 * objects.
	 * @param defaultName The name under which the default return value will be
	 * registered.
	 * @param capacity The initial registry capacity.
	 * @param overwrite Whether or not duplicate names and IDs overwrite old
	 * ones. If {@code false}, duplicate names and IDs are ignored.
	 * 
	 * @throws NullPointerException if either {@code name}, {@code
	 * defaultNamespace}, or {@code defaultName} are {@code null}.
	 * @throws IllegalArgumentException if {@code capacity < 0}.
	 */
	public RegistryNamespacedDefaulted(String name, String defaultNamespace, String defaultName,
			int capacity, boolean overwrite) {
		super(name, defaultNamespace, capacity, overwrite);
		if(defaultName == null)
			throw new NullPointerException("defaultName is null");
		this.defaultName = defaultName;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>If the name of the object matches that of the name of the default
	 * return value, the object will be set as the default return value.
	 * 
	 * @throws IndexOufOfBoundsException if {@code id < 0}.
	 * @throws NullPointerException if either {@code name} or {@code object}
	 * are {@code null}.
	 */
	@Override
	public void register(int id, String name, V object) {
		if(defaultName.equals(name)) {
			if(defaultValue == null) {
				defaultValue = object;
			} else {
				if(overwrite) {
					log.logCritical("Default value already exists; replacing old mapping");
					defaultValue = object;
				} else {
					log.logCritical("Default value already exists; ignoring new mapping");
				}
			}
		}
		
		super.register(id, name, object);
	}
	
	/**
	 * Gets the object to which the specified name is mapped.
	 * 
	 * @param name The name.
	 * 
	 * @return The object, or the default value (note that this may be {@code
	 * null}) if the name lacks a mapping.
	 */
	@Override
	public V get(String name) {
		V obj = super.get(name);
		return obj == null ? defaultValue : obj;
	}
	
	/**
	 * Gets the object to which the specified ID is mapped.
	 * 
	 * @param id The ID.
	 * 
	 * @return The object, or the default value (note that this may be {@code
	 * null}) if the ID lacks a mapping.
	 */
	@Override
	public V get(int id) {
		V obj = super.get(id);
		return obj == null ? defaultValue : obj;
	}
	
}