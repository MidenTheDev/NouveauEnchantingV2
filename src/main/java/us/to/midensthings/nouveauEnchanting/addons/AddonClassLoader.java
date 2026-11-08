package us.to.midensthings.nouveauEnchanting.addons;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/***
 * Loads an addon's classes. Classes are looked up in NouveauEnchanting (which includes the Paper API) first,
 * then in the addon's own jar, then in the plugins the addon depends on.
 * <p>
 * Paper only lets a plugin see the classes of plugins it depends on, so going straight to the dependency plugins'
 * class loaders is what lets addons use plugins that NouveauEnchanting itself doesn't depend on.
 * Addons are loaded before any plugins exist, so the dependency class loaders are added later, when NouveauEnchanting enables.
 */
public class AddonClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final List<ClassLoader> dependencyLoaders = new CopyOnWriteArrayList<>();

    public AddonClassLoader(Path jar, ClassLoader parent) throws IOException {
        super(new URL[]{jar.toUri().toURL()}, parent);
    }

    /***
     * Lets the addon use the classes of a plugin it depends on.
     */
    public void addDependencyLoader(ClassLoader dependencyLoader) {
        dependencyLoaders.add(dependencyLoader);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = findClassInOrder(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private Class<?> findClassInOrder(String name) throws ClassNotFoundException {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        for (ClassLoader dependencyLoader : dependencyLoaders) {
            try {
                return dependencyLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }
}
