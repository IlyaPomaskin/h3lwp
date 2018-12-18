package MapReader;

import java.util.BitSet;

public class DefInfo implements Cloneable {
    public DefInfo clone() {
        DefInfo newDefIno;

        try {
            newDefIno = (DefInfo) super.clone();
        } catch (CloneNotSupportedException ex) {
            newDefIno = new DefInfo();
        }

        return newDefIno;
    }

    public String spriteName;
    public BitSet passableCells;
    public BitSet activeCells;
    public int placementOrder;
    public int objectId;
    public int objectClassSubId;

    @Override
    public String toString() {
        return spriteName.toLowerCase().replace(".def", "");
    }

    public Boolean isVisitable() {
        return !activeCells.isEmpty();
    }
}
