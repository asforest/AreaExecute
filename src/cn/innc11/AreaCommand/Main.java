package cn.innc11.AreaCommand;

import cn.innc11.AreaCommand.datatype.CmdArea;
import cn.innc11.AreaCommand.datatype.PlayerClick;
import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main extends PluginBase implements Listener
{
    HashMap<String, CmdArea> areas = new HashMap<>();
    HashMap<String, PlayerClick> playerClicks = new HashMap<>();

    @Override
    public void onEnable()
    {
        areas.clear();
        playerClicks.clear();

        getServer().getPluginManager().registerEvents(this, this);

        saveResource("portals.yml", false);

        Config config = new Config(getDataFolder()+"/portals.yml");

        getLogger().info("Loaded "+config.getRootSection().getKeys(false).size()+" Areas");

        for(String key : config.getRootSection().getKeys(false))
        {
            CmdArea ca = new CmdArea();

            ca.world = config.getString(key+".world");

            double p1x = config.getDouble(key+".pos1.x");
            double p1y = config.getDouble(key+".pos1.y");
            double p1z = config.getDouble(key+".pos1.z");

            double p2x = config.getDouble(key+".pos2.x");
            double p2y = config.getDouble(key+".pos2.y");
            double p2z = config.getDouble(key+".pos2.z");

            ca.pos1 = new Vector3(p1x, p1y, p1z);
            ca.pos2 = new Vector3(p2x, p2y, p2z);

            ca.commandText = config.getString(key+".commandText");
            ca.isPlayerExecute = config.getBoolean(key+".isPlayerExecute");

            areas.put(key, ca);
        }
    }

    @Override
    public void onDisable()
    {
        saveResource("portals.yml", true);

        Config config = new Config(getDataFolder()+"/portals.yml");

        for(String key : areas.keySet())
        {
            CmdArea area = areas.get(key);

            config.set(key+".world", area.world);

            config.set(key+".pos1.x", area.pos1.x);
            config.set(key+".pos1.y", area.pos1.y);
            config.set(key+".pos1.z", area.pos1.z);

            config.set(key+".pos2.x", area.pos2.x);
            config.set(key+".pos2.y", area.pos2.y);
            config.set(key+".pos2.z", area.pos2.z);

            config.set(key+".commandText", area.commandText);

            config.set(key+".isPlayerExecute", area.isPlayerExecute);

        }

        config.save();

        areas.clear();
        playerClicks.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, cn.nukkit.command.Command command, String label, String[] args)
    {
        if(!sender.isPlayer())
        {
            sender.sendMessage("Can only be executed by a player");
            return true;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase())
        {
            case "addca": {

                if(args.length>=1)
                {
                    String AeraName = args[0];

                    if(!areas.containsKey(AeraName))
                    {
                        if(playerClicks.containsKey(player.getName()))
                        {
                            playerClicks.remove(player.getName());
                        }

                        playerClicks.put(player.getName(), new PlayerClick(AeraName));
                        player.sendMessage("Started to choose two-Point");
                        player.sendMessage("Please select two-Point in sneak mode");
                        break;
                    }else{
                        player.sendMessage("Ca "+AeraName+" already exists");
                        break;
                    }

                }else{
                    player.sendMessage("Need a ca name");
                    return false;
                }

            }

            case "delca":{
                if(args.length>0)
                {
                    if(areas.containsKey(args[0]))
                    {
                        areas.remove(args[0]);
                        player.sendMessage("Successfully removed ca: "+args[0]);
                    }else {
                        player.sendMessage("Not found ca: "+args[0]);
                    }
                }else{
                    player.sendMessage("Need a ca name");
                    return false;
                }
                break;
            }

            case "cmdca":{
                if(args.length>=1)
                {
                    String areaName = args[0];
                    String executer = args.length<=1?null:args[1];
                    StringBuffer cmmandTextBuffer = new StringBuffer();
                    boolean playerExec = false;

                    if(areas.containsKey(areaName))
                    {
                        CmdArea ca = areas.get(areaName);

                        if(args.length==1)
                        {
                            ca.commandText = "";

                            player.sendMessage("Successfully clean the command, "+areaName);
                        }else if(args.length>=3)
                        {
                            if(!executer.equals("server"))
                            {
                                playerExec = true;
                            }

                            for(int i=0;i<args.length-2;i++)
                            {
                                cmmandTextBuffer.append(args[i+2]+" ");
                            }

                            ca.isPlayerExecute = playerExec;
                            ca.commandText = cmmandTextBuffer.toString();

                            player.sendMessage("Successfully set the command :"+ca.commandText);
                            player.sendMessage("Command Executer: "+(playerExec?"Player":"Server"));
                            player.sendMessage("%PLAYER% will be replaced with the player name");

                        }else{
                            player.sendMessage("args too faw");
                            return false;
                        }

                    }else {
                        player.sendMessage("Not found ca: "+areaName);
                    }

                }else{
                    player.sendMessage("args too faw");
                    return false;
                }

                break;
            }

            case "listca":{
                player.sendMessage("----List of Ca----");
                for(Map.Entry<String, CmdArea> area : areas.entrySet())
                {
                    player.sendMessage(area.getKey()+"  command:("+area.getValue().commandText+")("+(area.getValue().isPlayerExecute?"Player":"Server")+")("+area.getValue().world+")");
                }
                break;
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEvent(PlayerInteractEvent event)
    {
        if(playerClicks.containsKey(event.getPlayer().getName()))
        {
            String playerName = event.getPlayer().getName();

            PlayerClick pc = playerClicks.get(playerName);

            if(event.getPlayer().isSneaking())
            {
                if(!pc.p1Setted)
                {
                    Location bLoc = event.getBlock().getLocation();
                    pc.p1 = new Position(bLoc.x, bLoc.y, bLoc.z, bLoc.level);
                    pc.p1Setted = true;
                    event.getPlayer().sendMessage("P1 Setted! "+pc.AeraName);
                    return;
                }

                if(!pc.p2Setted)
                {
                    Location bLoc = event.getBlock().getLocation();
                    pc.p2 = new Position(bLoc.x, bLoc.y, bLoc.z, bLoc.level);
                    pc.p2Setted = true;
                    event.getPlayer().sendMessage("P2 Setted! "+pc.AeraName);
                }

                if(pc.p1Setted && pc.p2Setted)
                {
                    CmdArea ca = new CmdArea();
                    ca.world = pc.p2.level.getName();

                    Vector3 Pmin = new Vector3(
                            Math.min(pc.p1.x, pc.p2.x),
                            Math.min(pc.p1.y, pc.p2.y),
                            Math.min(pc.p1.z, pc.p2.z));

                    Vector3 Pmax = new Vector3(
                            Math.max(pc.p1.x, pc.p2.x)+1,
                            Math.max(pc.p1.y, pc.p2.y),
                            Math.max(pc.p1.z, pc.p2.z)+1);

                    ca.pos1 = Pmin;
                    ca.pos2 = Pmax;

                    areas.put(pc.AeraName, ca);

                    playerClicks.remove(playerName);

                    event.getPlayer().sendMessage("Setted! "+pc.AeraName);
                }

            }else{
                playerClicks.remove(playerName);
                event.getPlayer().sendMessage("Point Picking is cancelled!");
            }



        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        for(Map.Entry<String, CmdArea> key : areas.entrySet())
        {
            CmdArea ca = key.getValue();
            Location loc = event.getTo();
            Location foc = event.getFrom();

            if(loc.x>=ca.pos1.x && loc.x<=ca.pos2.x &&
                    loc.y>=ca.pos1.y && loc.y<=ca.pos2.y &&
                    loc.z>=ca.pos1.z && loc.z<=ca.pos2.z &&

                    !(foc.x>=ca.pos1.x && foc.x<=ca.pos2.x &&
                            foc.y>=ca.pos1.y && foc.y<=ca.pos2.y &&
                            foc.z>=ca.pos1.z && foc.z<=ca.pos2.z)
            ){
                //event.getPlayer().sendActionBar("进入: "+key.getKey());
                //event.getPlayer().sendPopup("进入: "+key.getKey());



                if(ca.commandText!=null && !ca.commandText.isEmpty())
                {
                    String cmd = ca.commandText;
                    cmd = cmd.replaceAll("%PLAYER%", event.getPlayer().getName());

                    if(ca.isPlayerExecute)
                    {
                        getServer().dispatchCommand(event.getPlayer(), cmd);
                    }else{
                        getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
                    }
                }else{
                    if(event.getPlayer().isOp())
                        event.getPlayer().sendActionBar("Enter: "+key.getKey());
                }

            }

        }
    }
}
