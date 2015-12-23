package com.itextpdf.basics.font.otf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GposLookupType4 extends OpenTableLookup {

    private final List<MarkToBase> marksbases;

    public GposLookupType4(OpenTypeFontTableReader openReader, int lookupFlag, int[] subTableLocations) throws IOException {
        super(openReader, lookupFlag, subTableLocations);
        marksbases = new ArrayList<>();
        readSubTables();
    }

    @Override
    public boolean transformOne(GlyphLine line) {
        if (line.idx >= line.end)
            return false;
        if (openReader.isSkip(line.glyphs.get(line.idx).index, lookupFlag)) {
            line.idx++;
            return false;
        }

        boolean changed = false;
        GlyphIndexer gi = null;
        for (MarkToBase mb : marksbases) {
            OtfMarkRecord omr = mb.marks.get(line.glyphs.get(line.idx).index);
            if (omr == null)
                continue;
            if (gi == null) {
                gi = new GlyphIndexer();
                gi.idx = line.idx;
                gi.line = line;
                while (true) {
                    gi.previousGlyph(openReader, lookupFlag);
                    if (gi.glyph == null)
                        break;
                    // not mark => base glyph
                    if (!mb.marks.containsKey(gi.glyph.index))
                        break;
                }
                if (gi.glyph == null)
                    break;
            }
            GposAnchor[] gpas = mb.bases.get(gi.glyph.index);
            if (gpas == null)
                continue;
            int markClass = omr.markClass;
            GposAnchor baseAnchor = gpas[markClass];
            GposAnchor markAnchor = omr.anchor;
//            line.glyphs.add(line.idx, new Glyph(line.glyphs.get(line.idx),
//                    markAnchor.XCoordinate - baseAnchor.XCoordinate,
//                    markAnchor.YCoordinate - baseAnchor.YCoordinate,
//                    0, 0, gi.idx - line.idx));
            changed = true;
            break;
        }
        line.idx++;
        return changed;
    }

    @Override
    protected void readSubTable(int subTableLocation) throws IOException {
        openReader.rf.seek(subTableLocation);

        openReader.rf.readUnsignedShort(); //skip format, always 1
        int markCoverageLocation = openReader.rf.readUnsignedShort() + subTableLocation;
        int baseCoverageLocation = openReader.rf.readUnsignedShort() + subTableLocation;
        int classCount = openReader.rf.readUnsignedShort();
        int markArrayLocation = openReader.rf.readUnsignedShort() + subTableLocation;
        int baseArrayLocation = openReader.rf.readUnsignedShort() + subTableLocation;
        List<Integer> markCoverage = openReader.readCoverageFormat(markCoverageLocation);
        List<Integer> baseCoverage = openReader.readCoverageFormat(baseCoverageLocation);
        List<OtfMarkRecord> markRecords = OtfReadCommon.readMarkArray(openReader.rf, markArrayLocation);
        MarkToBase markToBase = new MarkToBase();
        for (int k = 0; k < markCoverage.size(); ++k) {
            markToBase.marks.put(markCoverage.get(k), markRecords.get(k));
        }
        List<GposAnchor[]> baseArray = readBaseArray(classCount, baseArrayLocation);
        for (int k = 0; k < baseCoverage.size(); ++k) {
            markToBase.bases.put(baseCoverage.get(k), baseArray.get(k));
        }
        marksbases.add(markToBase);
    }

    private List<GposAnchor[]> readBaseArray(int classCount, int location) throws IOException {
        ArrayList<GposAnchor[]> baseArray = new ArrayList<>();
        openReader.rf.seek(location);
        int baseCount = openReader.rf.readUnsignedShort();
        int[] anchorLocations = openReader.readUShortArray(baseCount * classCount, location);
        int idx = 0;
        for (int k = 0; k < baseCount; ++k) {
            GposAnchor[] anchors = new GposAnchor[classCount];
            for (int j = 0; j < classCount; ++j) {
                anchors[j] = OtfReadCommon.readGposAnchor(openReader.rf, anchorLocations[idx++]);
            }
            baseArray.add(anchors);
        }
        return baseArray;
    }

    public static class MarkToBase {
        public final Map<Integer, OtfMarkRecord> marks = new HashMap<>();
        public final Map<Integer, GposAnchor[]> bases = new HashMap<>();
    }
}