package com.mousebird.maply.imagerypro;

import com.mousebird.maply.CoordSystem;
import com.mousebird.maply.MaplyBaseController;
import com.mousebird.maply.RemoteTileInfo;

/**
 * A smarter version of the Multiplex tile source for Maply IPro.
 * <br>
 * This version of the MaplyMultiplexTileSource is slightly smarter about the data coming in.
 */
public class MultiplexTileSource extends com.mousebird.maply.MultiplexTileSource
{
    public MultiplexTileSource(MaplyBaseController inController, RemoteTileInfo[] inSources, CoordSystem inCoordSys)
    {
        super(inController,inSources,inCoordSys);
    }
}
