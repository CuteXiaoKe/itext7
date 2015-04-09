package com.itextpdf.canvas.color;

import com.itextpdf.basics.PdfException;
import com.itextpdf.core.pdf.colorspace.PdfColorSpace;
import com.itextpdf.core.pdf.colorspace.PdfPattern;
import com.itextpdf.core.pdf.colorspace.PdfSpecialCs;

import java.util.Objects;

public class PatternColor extends Color {

    private PdfPattern pattern;
    // The underlying color space for uncolored patterns. Will be null for colored ones.
    private PdfColorSpace underlyingCS;
    // The color value in underlying CS for uncolored patterns. Null for colored ones.
    private float[] colorValue;

    public PatternColor(PdfPattern coloredPattern) {
        super(new PdfSpecialCs.Pattern(), null);
        this.pattern = coloredPattern;
    }

    public PatternColor(PdfPattern.Tiling uncoloredPattern, Color color) throws PdfException {
        this(uncoloredPattern, color.getColorSpace(), color.getColorValue());
    }

    public PatternColor(PdfPattern.Tiling uncoloredPattern, PdfColorSpace underlyingCS, float[] colorValue) throws PdfException {
        super(new PdfSpecialCs.UncoloredTilingPattern(uncoloredPattern.getDocument(), underlyingCS), colorValue);
        if (underlyingCS instanceof PdfSpecialCs.Pattern)
            throw new IllegalArgumentException("underlyingCS");
        this.pattern = uncoloredPattern;
        this.underlyingCS = underlyingCS;
        this.colorValue = colorValue;
    }

    public PatternColor(PdfPattern.Tiling uncoloredPattern, PdfSpecialCs.UncoloredTilingPattern uncoloredTilingCS, float[] colorValue) throws PdfException {
        super(uncoloredTilingCS, colorValue);
        this.pattern = uncoloredPattern;
        this.underlyingCS = uncoloredTilingCS.getUnderlyingColorSpace();
        this.colorValue = colorValue;
    }

    public PdfPattern getPattern() {
        return pattern;
    }

    public void setPattern(PdfPattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || !super.equals(o)){
            return false;
        }
        return pattern.equals(((PatternColor)o).pattern) && Objects.equals(underlyingCS, ((PatternColor)o).underlyingCS)
                && Objects.equals(colorValue, ((PatternColor)o).colorValue);
    }
}
