package me.wang.premiumterminal.utils;

import com.google.common.collect.EvictingQueue;
import com.google.gson.Gson;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static me.wang.premiumterminal.PremiumTerminal.logFile;
import static me.wang.premiumterminal.PremiumTerminal.logconfig;

@SuppressWarnings("UnstableApiUsage")
public class Network {
    private final JavaPlugin plugin;
    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private BufferedReader reader;
    private OutputStream writer;
    private volatile boolean running = false;
    private volatile boolean sendid = true;
    private EvictingQueue<String> pluginLog = EvictingQueue.create(100);
    private Gson gson;

    private final String clientUUID;
    private final Password password;

    // 构造函数，初始化服务器地址、端口和插件实例
    public Network(JavaPlugin plugin, String serverHost, int serverPort) {
        String clientUUID1;
        this.gson = new Gson();
        this.plugin = plugin;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        clientUUID1 = plugin.getConfig().getString("uuid");
        if (clientUUID1 == null || clientUUID1.equalsIgnoreCase("")){
            clientUUID1 = UUID.randomUUID().toString();
            plugin.getConfig().set("uuid",clientUUID1);
            plugin.saveConfig();
            plugin.reloadConfig();
        }

        this.clientUUID = clientUUID1;
        this.password = new Password();
        if (plugin.getConfig().getString("password").equalsIgnoreCase("")){
            plugin.getConfig().set("password",password.toString());
            plugin.saveConfig();
            plugin.reloadConfig();
        }else {
            if (!password.setPassword(plugin.getConfig().getString("password"))){
                plugin.getLogger().severe("连接密钥不符合规则！");
                plugin.getConfig().set("password",password.toString());
                plugin.saveConfig();
                plugin.reloadConfig();
            }
        }
    }

    // 启动客户端，并在异步线程中运行
    public void start() {
        if (running) {
            Bukkit.getLogger().severe("错误：你不能在连接时重新建立连接！");
            return;
        }

        running = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getLogger().info(ChatColor.GREEN+"正在与服务器建立连接...");
                    socket = new Socket(serverHost, serverPort);
                    int timeout = socket.getSoTimeout();
                    socket.setSoTimeout(plugin.getConfig().getInt("connection.timeout"));
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    writer = socket.getOutputStream();
                    Bukkit.getLogger().info(ChatColor.AQUA+"已成功与服务器建立连接！");

                    // 发送 UUID 到服务器
                    if (sendid){
                        writer.write((clientUUID + "\n").getBytes(StandardCharsets.UTF_8));
                        writer.flush();
                        Bukkit.getLogger().info("设备UUID：" + clientUUID);
                        sendid = false;
                    }

                    socket.setSoTimeout(timeout);

                    // 读取服务器的消息，保持连接
                    String message;
                    if (!running){
                        Bukkit.getLogger().warning("连接已停止");
                    }
                    while (running) {

                        writeLog("waiting to msg");
                        message = reader.readLine();
                        if (message != null){
                            writeLog("R:"+message);

                            handleServerMessage(message);

                        }
                        writeLog("work");

                    }
                    writeLog("Loop end");
                } catch (IOException e) {
                    if (running) {
                        Bukkit.getLogger().severe("连接错误：" + e.getMessage());
                        writeLog("stop,code 1");
                    }
                } finally {
                    Bukkit.getLogger().warning("连接终止,正在重连...");
                    stop(true);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    // 处理来自服务器的消息
    private void handleServerMessage(String msg) {
        try {
            if (msg.startsWith("[SELECT]log")) {
                // 处理日志请求
                if (Terminal.queue.isEmpty()) {
                    writer.write("None\n".getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                    writeLog("return None");
                } else {
                    Log log = Terminal.queue.poll();
                    writer.write(("[LOG]" + log.toString() + "\n").getBytes(StandardCharsets.UTF_8));
                    writer.flush();

                    // 接收服务器的响应
                    String response = reader.readLine();
                    if (response != null) {
                        writeLog("RFL:"+response);
                        pluginLog.add(response);
                    }
                }
            } else if (msg.startsWith("[EXEC]")) {
                // 处理命令执行请求
                writeLog("CMD:"+msg);
                String command = msg.replace("[EXEC]", "").trim();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),command);
                });
            } else if (msg.startsWith("[REBOOT]")) {
                plugin.getLogger().info("正在重载服务器...");
                plugin.getServer().reload();
            } else if (msg.startsWith("[SHUTDOWN]")) {
                plugin.getLogger().info("正在关闭服务器...");
                plugin.getServer().shutdown();
            } else if (msg.startsWith("[SELECT]files")){
                String path = msg.replace("[SELECT]files", "").trim();
                String head = plugin.getConfig().getString("file.path");
                File file = new File(head+path);
                File[] files = file.listFiles();
                List<Map<String,String>> list = new ArrayList<>();

                if (files != null) {
                    for (File f:files){
                        Map<String,String> map = new HashMap<String, String>();
                        String name = f.getName();
                        map.put("name",name);
                        map.put("type",String.valueOf(f.isFile()));
                        list.add(map);
                    }
                }
                String s = gson.toJson(list);
                writer.write(("[FILES]"+ s).getBytes(StandardCharsets.UTF_8));
                writer.flush();
            }else if(msg.startsWith("[SELECT]plugins")){
                Plugin[] plugins = plugin.getServer().getPluginManager().getPlugins();
                List<Map<String,String>> list = new ArrayList<>();

                for (Plugin p : plugins) {
                    PluginDescriptionFile descriptionFile = p.getDescription();
                    Map<String, String> map = new HashMap<String, String>();
                    String name = p.getName();
                    map.put("name", name);
                    map.put("enable", String.valueOf(p.isEnabled()));
                    map.put("author", descriptionFile.getAuthors().toString());
                    map.put("version", descriptionFile.getVersion());
                    list.add(map);
                }
                String s = gson.toJson(list);
                writer.write(("[PLUGINS]"+ s).getBytes(StandardCharsets.UTF_8));
                writer.flush();
            }else if (msg.startsWith("[RELOAD]")){
                String name = msg.replace("[RELOAD]", "").trim();
                name = name.replaceAll("[\\r\\n]+$", "");
                Plugin pl = plugin.getServer().getPluginManager().getPlugin(name);
                if (pl != null) {
                    plugin.getServer().getPluginManager().disablePlugin(pl);
                }
                if (pl != null) {
                    plugin.getServer().getPluginManager().enablePlugin(pl);
                }
            }else if (msg.startsWith("[START]")){
                String name = msg.replace("[START]", "").trim();
                name = name.replaceAll("[\\r\\n]+$", "");
                Plugin pl = plugin.getServer().getPluginManager().getPlugin(name);
                if (pl != null) {
                    plugin.getServer().getPluginManager().enablePlugin(pl);
                }
            }else if (msg.startsWith("[DISABLE]")){
                String name = msg.replace("[DISABLE]", "").trim();
                name = name.replaceAll("[\\r\\n]+$", "");
                Plugin pl = plugin.getServer().getPluginManager().getPlugin(name);
                if (pl != null) {
                    plugin.getServer().getPluginManager().disablePlugin(pl);
                }
            }else if (msg.startsWith("[SELECT]system")){
                Map<String,Double> map = new HashMap<String, Double>();
                map.put("cpu",getCpuUsage());
                map.put("memory",getMemoryUsage());
                map.put("disk",getDiskUsage());
                String s = gson.toJson(map);
                writer.write(("[SYSTEM]"+ s).getBytes(StandardCharsets.UTF_8));
                writer.flush();
            } else if (msg.startsWith("[DELETE]file")) {
                String path = msg.replace("[DELETE]file", "").trim();
                File file = new File(path);
                if (!file.exists()){
                    writer.write(("[ERROR]file not found").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                    return;
                }
                if (file.delete()){
                    writer.write(("[INFO]file delete success").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                }else {
                    writer.write(("[INFO]file delete fail").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                }
            } else if (msg.startsWith("[VIEW]")){
                String path = msg.replace("[VIEW]", "").trim();
                File file = new File(path);
                if (!file.exists()){
                    writer.write(("[ERROR]file not found").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                    return;
                }
                StringBuilder content = new StringBuilder();
                final int MAX_SIZE = 1_048_567;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (content.length() + line.length() > MAX_SIZE) {
                            writer.write(("[ERROR]file too big").getBytes(StandardCharsets.UTF_8));
                            writer.flush();
                            return;
                        }
                        content.append(line).append("\n"); // 追加每一行并换行
                    }
                } catch (IOException e) {
                    writer.write(("[ERROR]file read error").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                    e.printStackTrace();
                    return;
                }
                String fileContent = content.toString();
                writer.write(("[CONTENT]"+fileContent).getBytes(StandardCharsets.UTF_8));
                writer.flush();
            }
        } catch (IOException e) {
            writeLog("error at get msg");
            Bukkit.getLogger().severe("在握手时发生错误: " + e.getMessage());
        }
    }

    // 停止客户端，关闭连接
    public void stop(Boolean restart) {
        running = false;
        try {

            if (socket != null && !socket.isClosed()) {
                if (reader != null){
                    writer.write(("[EXIT]\n").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                }

                socket.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            Bukkit.getLogger().info("连接已终止");
            if (restart){
                Bukkit.getLogger().info("正在重新启动...");
                Bukkit.getScheduler().runTask(plugin, this::start);
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("在与服务器终止连接时发生错误: " + e.getMessage());
            Bukkit.getLogger().info("正在重连...");
            stop(true);
        }
    }

    // 发送消息到服务器
    public void sendMessage(String message) {
        if (socket == null || socket.isClosed()) {
            Bukkit.getLogger().severe("错误：正在尝试向未连接的服务器发送请求");
            return;
        }
        try {
            writer.write((message + "\n").getBytes(StandardCharsets.UTF_8));
            writer.flush();
            Bukkit.getLogger().info("发送: " + message);
        } catch (IOException e) {
            Bukkit.getLogger().severe("在发送请求时出现错误: " + e.getMessage());
        }
    }

    private void writeLog(String message) {
        List<String> list = logconfig.getStringList("network");
        list.add(message);
        try {
            logconfig.save(logFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // 获取 CPU 使用率
    private static double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        // 获取系统 CPU 的总核数
        int availableProcessors = osBean.getAvailableProcessors();

        // 计算 CPU 使用率
        double cpuLoad = osBean.getSystemCpuLoad();
        return cpuLoad * 100;
        //System.out.printf("CPU 使用率: %.2f%% (总核数: %d)\n", cpuLoad * 100, availableProcessors);
    }

    // 获取内存使用率
    private static double getMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapMemoryUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMemoryMax = memoryMXBean.getHeapMemoryUsage().getMax();
        //long nonHeapMemoryUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        //long nonHeapMemoryMax = memoryMXBean.getNonHeapMemoryUsage().getMax();

        //double nonHeapUsage = (double) nonHeapMemoryUsed / nonHeapMemoryMax * 100;
        return (double) heapMemoryUsed / heapMemoryMax * 100;
        //System.out.printf("堆内存使用率: %.2f%% (已使用: %d bytes, 最大: %d bytes)\n", heapUsage, heapMemoryUsed, heapMemoryMax);
        //System.out.printf("非堆内存使用率: %.2f%% (已使用: %d bytes, 最大: %d bytes)\n", nonHeapUsage, nonHeapMemoryUsed, nonHeapMemoryMax);
    }

    // 获取存储占用率
    private static double getDiskUsage() {
        File root = new File("/");
        long totalSpace = root.getTotalSpace(); // 总空间
        long freeSpace = root.getFreeSpace();   // 可用空间
        long usedSpace = totalSpace - freeSpace; // 已使用空间

        return (double) usedSpace / totalSpace * 100;
        //System.out.printf("存储使用率: %.2f%% (已使用: %d bytes, 可用: %d bytes, 总计: %d bytes)\n", usage, usedSpace, freeSpace, totalSpace);
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public Password getPassword() {
        return password;
    }
}

