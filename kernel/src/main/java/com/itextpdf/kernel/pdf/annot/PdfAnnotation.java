/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2016 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.kernel.pdf.annot;

import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.layer.IPdfOCG;

public abstract class PdfAnnotation extends PdfObjectWrapper<PdfDictionary> {

    private static final long serialVersionUID = -6555705164241587799L;

    /**
     * Annotation flags.
     */
    public static final int INVISIBLE = 1;
    public static final int HIDDEN = 2;
    public static final int PRINT = 4;
    public static final int NO_ZOOM = 8;
    public static final int NO_ROTATE = 16;
    public static final int NO_VIEW = 32;
    public static final int READ_ONLY = 64;
    public static final int LOCKED = 128;
    public static final int TOGGLE_NO_VIEW = 256;
    public static final int LOCKED_CONTENTS = 512;

    /**
     * Annotation highlighting modes.
     */
    public static final PdfName HIGHLIGHT_NONE = PdfName.N;
    public static final PdfName HIGHLIGHT_INVERT = PdfName.I;
    public static final PdfName HIGHLIGHT_OUTLINE = PdfName.O;
    public static final PdfName HIGHLIGHT_PUSH = PdfName.P;
    public static final PdfName HIGHLIGHT_TOGGLE = PdfName.T;

    /**
     * Annotation highlighting modes.
     */
    public static final PdfName STYLE_SOLID = PdfName.S;
    public static final PdfName STYLE_DASHED = PdfName.D;
    public static final PdfName STYLE_BEVELED = PdfName.B;
    public static final PdfName STYLE_INSET = PdfName.I;
    public static final PdfName STYLE_UNDERLINE = PdfName.U;


    /**
     * Annotation states.
     */
    public static final PdfString Marked = new PdfString("Marked");
    public static final PdfString Unmarked = new PdfString("Unmarked");
    public static final PdfString Accepted = new PdfString("Accepted");
    public static final PdfString Rejected = new PdfString("Rejected");
    public static final PdfString Canceled = new PdfString("Cancelled");
    public static final PdfString Completed = new PdfString("Completed");
    public static final PdfString None = new PdfString("None");

    /**
     * Annotation state models.
     */
    public static final PdfString MarkedModel = new PdfString("Marked");
    public static final PdfString ReviewModel = new PdfString("Review");

    protected PdfPage page;

    public static PdfAnnotation makeAnnotation(PdfObject pdfObject, PdfAnnotation parent) {
        PdfAnnotation annotation = null;
        if (pdfObject.isIndirectReference())
            pdfObject = ((PdfIndirectReference) pdfObject).getRefersTo();
        if (pdfObject.isDictionary()) {
            PdfDictionary dictionary = (PdfDictionary) pdfObject;
            PdfName subtype = dictionary.getAsName(PdfName.Subtype);
            if (PdfName.Link.equals(subtype)) {
                annotation = new PdfLinkAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Popup.equals(subtype)) {
                annotation = new PdfPopupAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Widget.equals(subtype)) {
                annotation = new PdfWidgetAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Screen.equals(subtype)) {
                annotation = new PdfScreenAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName._3D.equals(subtype)) {
                throw new UnsupportedOperationException();
            } else if (PdfName.Highlight.equals(subtype) || PdfName.Underline.equals(subtype) || PdfName.Squiggly.equals(subtype) || PdfName.StrikeOut.equals(subtype)) {
                annotation = new PdfTextMarkupAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Caret.equals(subtype)) {
                annotation = new PdfCaretAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Text.equals(subtype)) {
                annotation = new PdfTextAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Sound.equals(subtype)) {
                annotation = new PdfSoundAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Stamp.equals(subtype)) {
                annotation = new PdfStampAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.FileAttachment.equals(subtype)) {
                annotation = new PdfFileAttachmentAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Ink.equals(subtype)) {
                annotation = new PdfInkAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.PrinterMark.equals(subtype)) {
                annotation = new PdfPrinterMarkAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.TrapNet.equals(subtype)) {
                annotation = new PdfTrapNetworkAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.FreeText.equals(subtype)) {
                annotation = new PdfFreeTextAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Square.equals(subtype)) {
                annotation = new PdfSquareAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Circle.equals(subtype)) {
                annotation = new PdfCircleAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Line.equals(subtype)) {
                annotation = new PdfLineAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Polygon.equals(subtype) || PdfName.PolyLine.equals(subtype)) {
                annotation = new PdfPolyGeomAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Redact.equals(subtype)) {
                annotation = new PdfRedactAnnotation((PdfDictionary) pdfObject);
            } else if (PdfName.Watermark.equals(subtype)) {
                annotation = new PdfWatermarkAnnotation((PdfDictionary) pdfObject);
            }
        }
        if (annotation instanceof PdfMarkupAnnotation) {
            PdfMarkupAnnotation markup = (PdfMarkupAnnotation) annotation;
            PdfDictionary inReplyTo = markup.getInReplyToObject();
            if (inReplyTo != null)
                markup.setInReplyTo(makeAnnotation(inReplyTo));
            PdfDictionary popup = markup.getPopupObject();
            if (popup != null)
                markup.setPopup((PdfPopupAnnotation) makeAnnotation(popup, markup));
        }
        if (annotation instanceof PdfPopupAnnotation) {
            PdfPopupAnnotation popup = (PdfPopupAnnotation) annotation;
            if (parent != null)
                popup.setParent(parent);
        }

        return annotation;
    }

    protected PdfAnnotation(Rectangle rect) {
        this(new PdfDictionary());
        put(PdfName.Rect, new PdfArray(rect));
        put(PdfName.Subtype, getSubtype());
    }

    protected PdfAnnotation(PdfDictionary pdfObject) {
        super(pdfObject);
        markObjectAsIndirect(getPdfObject());
    }

    public abstract PdfName getSubtype();

    /**
     * Sets the layer this annotation belongs to.
     *
     * @param layer the layer this annotation belongs to
     */
    public void setLayer(IPdfOCG layer) {
        getPdfObject().put(PdfName.OC, layer.getIndirectReference());
    }

    public PdfAnnotation setAction(PdfAction action) {
        return put(PdfName.A, action.getPdfObject());
    }

    public PdfAnnotation setAdditionalAction(PdfName key, PdfAction action) {
        PdfAction.setAdditionalAction(this, key, action);
        return this;
    }

    public PdfString getContents() {
        return getPdfObject().getAsString(PdfName.Contents);
    }

    public PdfAnnotation setContents(PdfString contents) {
        return put(PdfName.Contents, contents);
    }

    public PdfAnnotation setContents(String contents) {
        return setContents(new PdfString(contents));
    }

    public PdfDictionary getPageObject() {
        return getPdfObject().getAsDictionary(PdfName.P);
    }

    public PdfPage getPage() {
        if (page == null && getPdfObject().isIndirect()) {
            PdfIndirectReference annotationIndirectReference = getPdfObject().getIndirectReference();
            PdfDocument doc = annotationIndirectReference.getDocument();

            PdfDictionary pageDictionary = getPageObject();
            if (pageDictionary != null) {
                page = doc.getPage(pageDictionary);
            } else {
                for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                    PdfPage docPage = doc.getPage(i);
                    for (PdfAnnotation annot : docPage.getAnnotations()) {
                        if (annot.getPdfObject().getIndirectReference().equals(annotationIndirectReference)) {
                            page = docPage;
                            break;
                        }
                    }
                }
            }


        }
        return page;
    }

    public PdfAnnotation setPage(PdfPage page) {
        this.page = page;
        return put(PdfName.P, page.getPdfObject());
    }

    public PdfString getName() {
        return getPdfObject().getAsString(PdfName.NM);
    }

    public PdfAnnotation setName(PdfString name) {
        return put(PdfName.NM, name);
    }

    public PdfString getDate() {
        return getPdfObject().getAsString(PdfName.M);
    }

    public PdfAnnotation setDate(PdfString date) {
        return put(PdfName.M, date);
    }

    public int getFlags() {
        PdfNumber f = getPdfObject().getAsNumber(PdfName.F);
        if (f != null)
            return f.intValue();
        else
            return 0;
    }

    public PdfAnnotation setFlags(int flags) {
        return put(PdfName.F, new PdfNumber(flags));
    }

    public PdfAnnotation setFlag(int flag) {
        int flags = getFlags();
        flags = flags | flag;
        return setFlags(flags);
    }

    public PdfAnnotation resetFlag(int flag) {
        int flags = getFlags();
        flags = flags & (~flag & 0xff);
        return setFlags(flags);
    }

    public boolean hasFlag(int flag) {
        int flags = getFlags();
        return (flags & flag) != 0;
    }

    public PdfDictionary getAppearanceDictionary() {
        return getPdfObject().getAsDictionary(PdfName.AP);
    }

    public PdfDictionary getAppearanceObject(PdfName appearanceType) {
        PdfDictionary ap = getAppearanceDictionary();
        if (ap != null) {
            return ap.getAsDictionary(appearanceType);
        }
        return null;
    }

    public PdfDictionary getNormalAppearanceObject() {
        return getAppearanceObject(PdfName.N);
    }

    public PdfDictionary getRolloverAppearanceObject() {
        return getAppearanceObject(PdfName.R);
    }

    public PdfDictionary getDownAppearanceObject() {
        return getAppearanceObject(PdfName.D);
    }

    public PdfAnnotation setAppearance(PdfName appearanceType, PdfDictionary appearance) {
        PdfDictionary ap = getAppearanceDictionary();
        if (ap == null) {
            ap = new PdfDictionary();
            getPdfObject().put(PdfName.AP, ap);
        }
        ap.put(appearanceType, appearance);
        return this;
    }

    public PdfAnnotation setNormalAppearance(PdfDictionary appearance) {
        return setAppearance(PdfName.N, appearance);
    }

    public PdfAnnotation setRolloverAppearance(PdfDictionary appearance) {
        return setAppearance(PdfName.R, appearance);
    }

    public PdfAnnotation setDownAppearance(PdfDictionary appearance) {
        return setAppearance(PdfName.D, appearance);
    }

    public PdfAnnotation setAppearance(PdfName appearanceType, PdfAnnotationAppearance appearance) {
        return setAppearance(appearanceType, appearance.getPdfObject());
    }

    public PdfAnnotation setNormalAppearance(PdfAnnotationAppearance appearance) {
        return setAppearance(PdfName.N, appearance);
    }

    public PdfAnnotation setRolloverAppearance(PdfAnnotationAppearance appearance) {
        return setAppearance(PdfName.R, appearance);
    }

    public PdfAnnotation setDownAppearance(PdfAnnotationAppearance appearance) {
        return setAppearance(PdfName.D, appearance);
    }

    public PdfName getAppearanceState() {
        return getPdfObject().getAsName(PdfName.AS);
    }

    public PdfAnnotation setAppearanceState(PdfName as) {
        return put(PdfName.AS, as);
    }

    public PdfArray getBorder() {
        return getPdfObject().getAsArray(PdfName.Border);
    }

    public PdfAnnotation setBorder(PdfArray border) {
        return put(PdfName.Border, border);
    }

    public PdfArray getColorObject() {
        return getPdfObject().getAsArray(PdfName.C);
    }

    public PdfAnnotation setColor(PdfArray color) {
        return put(PdfName.C, color);
    }

    public PdfAnnotation setColor(float[] color) {
        return setColor(new PdfArray(color));
    }

    public PdfAnnotation setColor(Color color) {
        return setColor(new PdfArray(color.getColorValue()));
    }

    public int getStructParentIndex() {
        PdfNumber n = getPdfObject().getAsNumber(PdfName.StructParent);
        if (n == null)
            return -1;
        else
            return n.intValue();
    }

    public PdfAnnotation setStructParentIndex(int structParentIndex) {
        return put(PdfName.StructParent, new PdfNumber(structParentIndex));
    }

    public PdfArray getQuadPoints() {
        return getPdfObject().getAsArray(PdfName.QuadPoints);
    }

    public boolean getOpen() {
        PdfBoolean open = getPdfObject().getAsBoolean(PdfName.Open);
        return open != null && open.getValue();
    }

    public PdfAnnotation setOpen(boolean open) {
        return put(PdfName.Open, new PdfBoolean(open));
    }

    public PdfAnnotation setQuadPoints(PdfArray quadPoints) {
        return put(PdfName.QuadPoints, quadPoints);
    }

    public PdfAnnotation setBorderStyle(PdfDictionary borderStyle) {
        return put(PdfName.BS, borderStyle);
    }

    /**
     * Setter for the annotation's border style. Possible values are
     * <ul>
     *     <li>{@link PdfAnnotation#STYLE_SOLID} - A solid rectangle surrounding the annotation.</li>
     *     <li>{@link PdfAnnotation#STYLE_DASHED} - A dashed rectangle surrounding the annotation.</li>
     *     <li>{@link PdfAnnotation#STYLE_BEVELED} - A simulated embossed rectangle that appears to be raised above the surface of the page.</li>
     *     <li>{@link PdfAnnotation#STYLE_INSET} - A simulated engraved rectangle that appears to be recessed below the surface of the page.</li>
     *     <li>{@link PdfAnnotation#STYLE_UNDERLINE} - A single line along the bottom of the annotation rectangle.</li>
     * </ul>
     * @param style The new value for the annotation's border style.
     * @return The annotation which this method was called on.
     */
    public PdfAnnotation setBorderStyle(PdfName style) {
        PdfDictionary styleDict = getBorderStyle();
        if (null == styleDict) {
            styleDict = new PdfDictionary();
        }
        styleDict.put(PdfName.S, style);
        return setBorderStyle(styleDict);
    }

    public PdfAnnotation setDashPattern(PdfArray dashPattern) {
        PdfDictionary styleDict = getBorderStyle();
        if (null == styleDict) {
            styleDict = new PdfDictionary();
        }
        styleDict.put(PdfName.D, dashPattern);
        return setBorderStyle(styleDict);
    }

    public PdfDictionary getBorderStyle() {
        return getPdfObject().getAsDictionary(PdfName.BS);
    }

    public static PdfAnnotation makeAnnotation(PdfObject pdfObject) {
        return makeAnnotation(pdfObject, null);
    }

    public PdfAnnotation setTitle(PdfString title) {
        return put(PdfName.T, title);
    }

    public PdfString getTitle() {
        return getPdfObject().getAsString(PdfName.T);
    }

    public PdfAnnotation setAppearanceCharacteristics(PdfDictionary characteristics) {
        return put(PdfName.MK, characteristics);
    }

    public PdfDictionary getAppearanceCharacteristics() {
        return getPdfObject().getAsDictionary(PdfName.MK);
    }

    public PdfDictionary getAction() {
        return getPdfObject().getAsDictionary(PdfName.A);
    }

    public PdfDictionary getAdditionalAction() {
        return getPdfObject().getAsDictionary(PdfName.AA);
    }

    public PdfAnnotation setRectangle(PdfArray array){
        return put(PdfName.Rect, array);
    }

    public PdfArray getRectangle() {
        return getPdfObject().getAsArray(PdfName.Rect);
    }

    public PdfAnnotation put(PdfName key, PdfObject value) {
        getPdfObject().put(key, value);
        return this;
    }

    public PdfAnnotation remove(PdfName key) {
        getPdfObject().remove(key);
        return this;
    }

    /**
     * To manually flush a {@code PdfObject} behind this wrapper, you have to ensure
     * that this object is added to the document, i.e. it has an indirect reference.
     * Basically this means that before flushing you need to explicitly call {@link #makeIndirect(PdfDocument)}.
     * For example: wrapperInstance.makeIndirect(document).flush();
     * Note that not every wrapper require this, only those that have such warning in documentation.
     */
    @Override
    public void flush() {
        super.flush();
    }

    @Override
    protected boolean isWrappedObjectMustBeIndirect() {
        return true;
    }
}
