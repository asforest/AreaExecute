package cn.innc11.AreaCommand.datatype;

import cn.nukkit.level.Position;

public class PlayerClick
{
    public PlayerClick(String AeraName)
    {
        this.AeraName = AeraName;
    }

    public String AeraName;

    public Position p1;
    public Position p2;

    public boolean p1Setted = false;
    public boolean p2Setted = false;
}
