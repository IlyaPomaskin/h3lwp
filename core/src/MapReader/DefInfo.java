package MapReader;

import java.util.BitSet;

public class DefInfo {

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
