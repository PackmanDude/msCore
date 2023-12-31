package com.github.minersstudios.mscore;

import com.github.minersstudios.mscore.command.Commodore;
import com.github.minersstudios.mscore.command.MSCommand;
import com.github.minersstudios.mscore.command.MSCommandExecutor;
import com.github.minersstudios.mscore.listener.MSListener;
import com.google.common.base.Charsets;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "EmptyMethod"})
public abstract class MSPlugin extends JavaPlugin {
    protected File pluginFolder;
    protected File configFile;
    protected FileConfiguration newConfig;
    protected boolean loadedCustoms;
    protected Set<String> classNames;
    protected Map<MSCommand, MSCommandExecutor> msCommands;
    protected Set<Listener> listeners;
    protected Commodore commodore;

    @Override
    public final void onLoad() {
        this.pluginFolder = new File("config/minersstudios/" + this.getName() + "/");
        this.configFile = new File(this.pluginFolder, "config.yml");
        this.loadedCustoms = false;

        long time = System.currentTimeMillis();

        this.loadClassNames();
        this.loadCommands();
        this.loadListeners();

        System.out.println(System.currentTimeMillis() - time);

        try {
            Field field = JavaPlugin.class.getDeclaredField("dataFolder");
            field.setAccessible(true);
            field.set(this, this.pluginFolder);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.load();
    }

    @Override
    public final void onEnable() {
        long time = System.currentTimeMillis();
        this.commodore = new Commodore(this);

        this.registerCommands();
        this.registerListeners();
        this.enable();
        this.getLogger().log(Level.INFO, "\033[0;92mEnabled in " + (System.currentTimeMillis() - time) + "ms");
    }

    @Override
    public final void onDisable() {
        long time = System.currentTimeMillis();

        this.disable();
        this.getLogger().log(Level.INFO, "\033[0;92mDisabled in " + (System.currentTimeMillis() - time) + "ms");
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        if (this.newConfig == null) {
            this.reloadConfig();
        }
        return this.newConfig;
    }

    @Override
    public void reloadConfig() {
        this.newConfig = YamlConfiguration.loadConfiguration(this.configFile);
        InputStream defConfigStream = this.getResource("config.yml");

        if (defConfigStream == null) return;

        InputStreamReader inputStreamReader = new InputStreamReader(defConfigStream, Charsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(inputStreamReader);

        this.newConfig.setDefaults(configuration);
    }

    @Override
    public void saveConfig() {
        try {
            this.getConfig().save(this.configFile);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
        }
    }

    @Override
    public void saveResource(
            @NotNull String resourcePath,
            boolean replace
    ) throws IllegalArgumentException {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        String path = resourcePath.replace('\\', '/');
        InputStream in = this.getResource(path);

        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + path + "' cannot be found");
        }

        String dirPath = path.substring(0, Math.max(path.lastIndexOf('/'), 0));
        File outFile = new File(this.pluginFolder, path);
        File outDir = new File(this.pluginFolder, dirPath);
        String outFileName = outFile.getName();
        String outDirName = outFile.getName();

        if (!outDir.exists()) {
            boolean created = outDir.mkdirs();
            if (!created) {
                this.getLogger().log(Level.WARNING, "Directory " + outDirName + " creation failed");
            }
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                out.close();
                in.close();
            } else {
                this.getLogger().log(Level.WARNING, "Could not save " + outFileName + " to " + outFile + " because " + outFileName + " already exists.");
            }
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save " + outFileName + " to " + outFile, ex);
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (!this.configFile.exists()) {
            this.saveResource("config.yml", false);
        }
    }

    public void loadCommands() {
        Logger logger = this.getLogger();
        this.msCommands = new HashMap<>();

        this.classNames.stream().parallel().forEach(className -> {
            try {
                Class<?> clazz = this.getClassLoader().loadClass(className);
                MSCommand msCommand = clazz.getAnnotation(MSCommand.class);

                if (msCommand != null) {
                    if (clazz.getDeclaredConstructor().newInstance() instanceof MSCommandExecutor msCommandExecutor) {
                        this.msCommands.put(msCommand, msCommandExecutor);
                    } else {
                        logger.log(Level.WARNING, "Loaded command that is not instance of MSCommandExecutor (" + className + ")");
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load command", e);
            }
        });
    }

    /**
     * Registers all command in the project that is annotated with {@link MSCommand}
     * <p>
     * All commands must be implemented using {@link MSCommandExecutor}
     */
    public void registerCommands() {
        this.msCommands.forEach(this::registerCommand);
    }

    /**
     * @param msCommand command to be registered
     * @param executor  command executor
     */
    public final void registerCommand(
            @NotNull MSCommand msCommand,
            @NotNull MSCommandExecutor executor
    ) throws IllegalArgumentException {
        String name = msCommand.command();
        CommandNode<?> commandNode = executor.getCommandNode();
        PluginCommand bukkitCommand = this.getCommand(name);
        PluginCommand pluginCommand = bukkitCommand == null ? createCommand(name) : bukkitCommand;

        if (pluginCommand == null) {
            this.getLogger().log(Level.SEVERE, "Failed to register command: " + name);
            return;
        }

        List<String> aliases = Arrays.asList(msCommand.aliases());
        String usage = msCommand.usage();
        String description = msCommand.description();
        String permissionStr = msCommand.permission();

        if (!aliases.isEmpty()) {
            pluginCommand.setAliases(aliases);
        }

        if (!usage.isEmpty()) {
            pluginCommand.setUsage(usage);
        }

        if (!description.isEmpty()) {
            pluginCommand.setDescription(description);
        }

		if (!permissionStr.isEmpty()) {
			PluginManager pluginManager = this.getServer().getPluginManager();
			Map<String, Boolean> children = new HashMap<>();
			String[] keys = msCommand.permissionParentKeys();
			boolean[] values = msCommand.permissionParentValues();

            if (keys.length != values.length) {
                throw new IllegalArgumentException("Permission and boolean array lengths do not match in command : " + name);
            } else {
                for (int i = 0; i < keys.length; i++) {
                    children.put(keys[i], values[i]);
                }
            }

            if (pluginManager.getPermission(permissionStr) == null) {
                Permission permission = new Permission(permissionStr, msCommand.permissionDefault(), children);
                pluginManager.addPermission(permission);
            }

            pluginCommand.setPermission(permissionStr);
        }

        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);

        if (commandNode != null) {
            this.commodore.register(pluginCommand, (LiteralCommandNode<?>) commandNode);
        }

        this.getServer().getCommandMap().register(this.getName(), pluginCommand);
    }

    private @Nullable PluginCommand createCommand(@NotNull String command) throws RuntimeException {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            return constructor.newInstance(command, this);
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "Failed to create command: " + command, e);
            return null;
        }
    }

    public void loadListeners() {
        Logger logger = this.getLogger();
        this.listeners = new HashSet<>();

        this.classNames.stream().parallel().forEach(className -> {
            try {
                Class<?> clazz = this.getClassLoader().loadClass(className);

                if (clazz.isAnnotationPresent(MSListener.class)) {
                    if (clazz.getDeclaredConstructor().newInstance() instanceof Listener listener) {
                        this.listeners.add(listener);
                    } else {
                        logger.log(Level.WARNING, "Registered listener that is not instance of Listener (" + className + ")");
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load listener", e);
            }
        });
    }

    /**
     * Registers all listeners in the project that is annotated with {@link MSListener}
     * <p>
     * All listeners must be implemented using {@link Listener}
     */
    public void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        this.listeners.forEach(listener -> pluginManager.registerEvents(listener, this));
    }

    private void loadClassNames() {
        try (JarFile jarFile = new JarFile(this.getFile())) {
            this.classNames = jarFile.stream().parallel()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .map(name -> name.replace("/", ".").replace(".class", ""))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gathers the names of all plugin classes and converts them to a package-like string
     * <p>
     * "com/example/Example.class" -> "com.example.Example"
     *
     * @return plugin class names
     */
    public final @NotNull Set<String> getClassNames() {
        return this.classNames;
    }

    public void load() {}

    public void enable() {}

    public void disable() {}

    @Contract(pure = true)
    public final @NotNull File getConfigFile() {
        return this.configFile;
    }

    @Contract(pure = true)
    public final @NotNull File getPluginFolder() {
        return this.pluginFolder;
    }

    @Contract(pure = true)
    public final boolean isLoadedCustoms() {
        return this.loadedCustoms;
    }

    public final void setLoadedCustoms(boolean loadedCustoms) {
        this.loadedCustoms = loadedCustoms;
    }
}
