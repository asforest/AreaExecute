package cn.innc11.areaexecute;

import cn.innc11.areaexecute.structure.Creat;
import cn.innc11.areaexecute.structure.Area;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.util.Arrays;
import java.util.HashMap;

public class Main extends PluginBase implements Listener
{
    HashMap<String, Area> areas = new HashMap<>();
    HashMap<String, Creat> creating = new HashMap<>();

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        reload();
    }

    void reload()
    {
        areas.clear();
        creating.clear();

        saveDefaultConfig();

        Config config = getConfig();
        config.reload();

        int c = 0;
        for(String label : config.getSection("areas").getKeys(false))
        {
            String prefix = "areas."+label;
            double x1 = config.getDouble(prefix+".x1");
            double y1 = config.getDouble(prefix+".y1");
            double z1 = config.getDouble(prefix+".z1");
            double x2 = config.getDouble(prefix+".x2");
            double y2 = config.getDouble(prefix+".y2");
            double z2 = config.getDouble(prefix+".z2");
            String world = config.getString(prefix+".world");
            String command = config.getString(prefix+".command");
            boolean isExecuteAsPlayer = config.getBoolean(prefix+".isExecuteByPlayer");

            Area area = new Area();
            area.pos1 = new Vector3(x1, y1, z1);
            area.pos2 = new Vector3(x2, y2, z2);
            area.world = world;
            area.command = command;
            area.isExecuteByPlayer = isExecuteAsPlayer;

            areas.put(label, area);
            c++;
        }

        getLogger().info(TextFormat.colorize("Loaded &a"+c+"&r areas"));
    }

    void save()
    {
        Config config = getConfig();

        areas.forEach((name, area)->
        {
            String prefix = "areas."+name;

            config.set(prefix+".x1", area.pos1.x);
            config.set(prefix+".y1", area.pos1.y);
            config.set(prefix+".z1", area.pos1.z);
            config.set(prefix+".x2", area.pos2.x);
            config.set(prefix+".y2", area.pos2.y);
            config.set(prefix+".z2", area.pos2.z);
            config.set(prefix+".world", area.world);
            config.set(prefix+".command", area.command);
            config.set(prefix+".isExecuteByPlayer", area.isExecuteByPlayer);
        });

        config.save();
    }

    void sendHelp(Player player)
    {
        player.sendMessage(TextFormat.colorize("&a------AreaExecute------"));
        player.sendMessage(TextFormat.colorize("&3/ae set <name> <p/s> <command>  创建/更新"));
        player.sendMessage(TextFormat.colorize("&3/ae remove <name>   移除"));
        player.sendMessage(TextFormat.colorize("&3/ae list   列表"));
        player.sendMessage(TextFormat.colorize("&3/ae reload   重新加载"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!sender.isPlayer())
        {
            sender.sendMessage("后台无法执行这个指令");
            return true;
        }

        if(!sender.isOp())
        {
            return true;
        }

        Player player = (Player) sender;

        if(args.length==0)
        {
            sendHelp(player);
            return true;
        }

        switch (args[0])
        {
            case "set":
            {
                if(args.length>=3)
                {
                    String name = args[1];
                    boolean exByPlayer = !args[2].equals("s");
                    String Command = (args.length>3)? String.join(" ", Arrays.copyOfRange(args, 3, args.length)):"";

                    if(areas.containsKey(name))
                    {
                        areas.get(name).isExecuteByPlayer = exByPlayer;
                        areas.get(name).command = Command;
                        player.sendMessage(TextFormat.colorize("已更新"+name+" | "+(exByPlayer?"&a玩家":"&4&l后台")+"&r执行 | 指令: &e"+(Command.isEmpty()?"&3未设置":Command)));
                        player.sendMessage("%PLAYER% 参数会被替换成实际玩家名字");
                        save();
                    }else{
                        Creat cr = new Creat();
                        cr.areaName = name;
                        cr.command = Command;
                        cr.executeByPlayer = exByPlayer;
                        creating.put(player.getName(), cr);
                        player.sendMessage("请点击两个点来确定一个区域");
                    }

                }else{
                    player.sendMessage("参数个数不正确(1)");
                }
                break;
            }

            case "remove":
            {
                if(args.length>=2)
                {
                    String name = args[1];

                    if(areas.containsKey(name))
                    {
                        player.sendMessage("已移除"+name);
                        areas.remove(name);
                        save();
                    }else{
                        player.sendMessage("找不到"+name);
                    }
                }else{
                    player.sendMessage("参数格式不正确(2)");
                }
                break;
            }

            case "list":
            {
                player.sendMessage("----AreaExecute列表----");

                areas.forEach((name, area)->
                {
                    boolean exByPlayer = area.isExecuteByPlayer;
                    String Command = area.command;

                    player.sendMessage(TextFormat.colorize("- "+name+" | "+(exByPlayer?"&a玩家":"&4&l后台")+"&r执行 | 世界: "+area.world+" | 指令: (&e"+(Command.isEmpty()?"未设置":Command)+"&r)"));

                });

                break;
            }

            case "reload":
            {
                reload();
                player.sendMessage("Reload Done");
                break;
            }

            default:
            {
                sendHelp(player);
                break;
            }
        }

        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        Player player = e.getPlayer();
        String playerName = player.getName();

        if(creating.containsKey(playerName))
        {
            Creat cr = creating.get(playerName);

            if(cr.p1==null)
            {
                Position loc = e.getBlock().getLocation();
                cr.p1 = loc.clone();
                player.sendMessage("点1已设置("+cr.areaName+")");
                return;
            }

            if(cr.p2==null)
            {
                Position loc = e.getBlock().getLocation();
                cr.p2 = loc.clone();
                player.sendMessage("点2已设置("+cr.areaName+")");

                if(!cr.p1.level.equals(cr.p2.level))
                {
                    player.sendMessage("点1和点2必须要在同一个世界里");
                    creating.remove(playerName);
                    return;
                }

                Area a = new Area();
                a.world = cr.p2.level.getName();

                Vector3 PositionMin = new Vector3(
                        Math.min(cr.p1.x, cr.p2.x),
                        Math.min(cr.p1.y, cr.p2.y),
                        Math.min(cr.p1.z, cr.p2.z));

                Vector3 PositionMax = new Vector3(
                        Math.max(cr.p1.x, cr.p2.x)+1,
                        Math.max(cr.p1.y, cr.p2.y),
                        Math.max(cr.p1.z, cr.p2.z)+1);

                a.pos1 = PositionMin;
                a.pos2 = PositionMax;
                a.command = cr.command;
                a.isExecuteByPlayer = cr.executeByPlayer;

                areas.put(cr.areaName, a);
                creating.remove(playerName);
                player.sendMessage("点1和点2设置完成");
                save();
            }

        }
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        areas.forEach((name, a)->
        {
            Player player = e.getPlayer();
            Location to = e.getTo();
            Location from = e.getFrom();

            boolean walkIn =
                    to.x>=a.pos1.x && to.x<=a.pos2.x &&
                    to.y>=a.pos1.y && to.y<=a.pos2.y &&
                    to.z>=a.pos1.z && to.z<=a.pos2.z &&
                    !(from.x>=a.pos1.x && from.x<=a.pos2.x &&
                      from.y>=a.pos1.y && from.y<=a.pos2.y &&
                      from.z>=a.pos1.z && from.z<=a.pos2.z);

            if(walkIn)
            {
                if(!a.command.isEmpty())
                {
                    String cmd = a.command.replaceAll("%PLAYER%", player.getName());
                    CommandSender executor = a.isExecuteByPlayer?player:getServer().getConsoleSender();

                    getServer().dispatchCommand(executor, cmd);
                }else{
                    if(player.isOp()){
                        player.sendActionBar("Enter: "+name);
                    }
                }
            }

        });

    }
}
