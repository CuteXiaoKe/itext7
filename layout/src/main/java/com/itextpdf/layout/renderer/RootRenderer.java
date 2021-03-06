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
package com.itextpdf.layout.renderer;

import com.itextpdf.io.LogMessageConstant;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.property.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class RootRenderer extends AbstractRenderer {

    protected boolean immediateFlush = true;
    protected LayoutArea currentArea;
    protected int currentPageNumber;
    private IRenderer keepWithNextHangingRenderer;
    private LayoutResult keepWithNextHangingRendererLayoutResult;

    public void addChild(IRenderer renderer) {
        super.addChild(renderer);

        if (currentArea == null) {
            updateCurrentArea(null);
        }

        // Static layout
        if (currentArea != null && !childRenderers.isEmpty() && childRenderers.get(childRenderers.size() - 1) == renderer) {
            childRenderers.remove(childRenderers.size() - 1);

            processWaitingKeepWithNextElement(renderer);

            List<IRenderer> resultRenderers = new ArrayList<>();
            LayoutResult result = null;

            LayoutArea storedArea = null;
            LayoutArea nextStoredArea = null;
            while (currentArea != null && renderer != null && (result = renderer.setParent(this).layout(new LayoutContext(currentArea.clone()))).getStatus() != LayoutResult.FULL) {
                if (result.getStatus() == LayoutResult.PARTIAL) {
                    if (result.getOverflowRenderer() instanceof ImageRenderer) {
                        ((ImageRenderer) result.getOverflowRenderer()).autoScale(currentArea);
                    } else {
                        processRenderer(result.getSplitRenderer(), resultRenderers);
                        if (nextStoredArea != null) {
                            currentArea = nextStoredArea;
                            currentPageNumber = nextStoredArea.getPageNumber();
                            nextStoredArea = null;
                        } else {
                            updateCurrentArea(result);
                        }
                    }
                } else if (result.getStatus() == LayoutResult.NOTHING) {
                    if (result.getOverflowRenderer() instanceof ImageRenderer) {
                        if (currentArea.getBBox().getHeight() < ((ImageRenderer) result.getOverflowRenderer()).imageHeight && !currentArea.isEmptyArea()) {
                            updateCurrentArea(result);
                        }
                        ((ImageRenderer)result.getOverflowRenderer()).autoScale(currentArea);
                        result.getOverflowRenderer().setProperty(Property.FORCED_PLACEMENT, true);
                    } else {
                        if (currentArea.isEmptyArea() && !(renderer instanceof AreaBreakRenderer)) {
                            if (Boolean.TRUE.equals(result.getOverflowRenderer().getModelElement().<Boolean>getProperty(Property.KEEP_TOGETHER))) {
                                result.getOverflowRenderer().getModelElement().setProperty(Property.KEEP_TOGETHER, false);
                                Logger logger = LoggerFactory.getLogger(RootRenderer.class);
                                logger.warn(MessageFormat.format(LogMessageConstant.ELEMENT_DOES_NOT_FIT_AREA, "KeepTogether property will be ignored."));
                                if (storedArea != null) {
                                    nextStoredArea = currentArea;
                                    currentArea = storedArea;
                                    currentPageNumber = storedArea.getPageNumber();
                                }
                                storedArea = currentArea;
                            } else if (null != result.getCauseOfNothing() && Boolean.TRUE.equals(result.getCauseOfNothing().<Boolean>getProperty(Property.KEEP_TOGETHER))) {
                                // set KEEP_TOGETHER false on the deepest parent (maybe the element itself) to have KEEP_TOGETHER == true
                                IRenderer theDeepestKeptTogether = result.getCauseOfNothing();
                                while (null == theDeepestKeptTogether.getModelElement() || null == theDeepestKeptTogether.getModelElement().<Boolean>getOwnProperty(Property.KEEP_TOGETHER)) {
                                    theDeepestKeptTogether = theDeepestKeptTogether.getParent();
                                }
                                theDeepestKeptTogether.getModelElement().setProperty(Property.KEEP_TOGETHER, false);
                                Logger logger = LoggerFactory.getLogger(RootRenderer.class);
                                logger.warn(MessageFormat.format(LogMessageConstant.ELEMENT_DOES_NOT_FIT_AREA, "KeepTogether property of inner element will be ignored."));
                            } else
                            {
                                result.getOverflowRenderer().setProperty(Property.FORCED_PLACEMENT, true);
                                Logger logger = LoggerFactory.getLogger(RootRenderer.class);
                                logger.warn(MessageFormat.format(LogMessageConstant.ELEMENT_DOES_NOT_FIT_AREA, ""));
                            }
                            renderer = result.getOverflowRenderer();

                            continue;
                        }
                        storedArea = currentArea;
                        if (nextStoredArea != null) {
                            currentArea = nextStoredArea;
                            currentPageNumber = nextStoredArea.getPageNumber();
                            nextStoredArea = null;
                        } else {
                            updateCurrentArea(result);
                        }
                    }
                }
                renderer = result.getOverflowRenderer();
            }

            // Keep renderer until next element is added for future keep with next adjustments
            if (renderer != null && Boolean.TRUE.equals(renderer.<Boolean>getProperty(Property.KEEP_WITH_NEXT))) {
                if (Boolean.TRUE.equals(renderer.<Boolean>getProperty(Property.FORCED_PLACEMENT))) {
                    Logger logger = LoggerFactory.getLogger(RootRenderer.class);
                    logger.warn(LogMessageConstant.ELEMENT_WAS_FORCE_PLACED_KEEP_WITH_NEXT_WILL_BE_IGNORED);
                    updateCurrentAreaAndProcessRenderer(renderer, resultRenderers, result);
                } else {
                    keepWithNextHangingRenderer = renderer;
                    keepWithNextHangingRendererLayoutResult = result;
                }
            } else {
                updateCurrentAreaAndProcessRenderer(renderer, resultRenderers, result);
            }
        } else if (positionedRenderers.size() > 0 && positionedRenderers.get(positionedRenderers.size() - 1) == renderer) {
            Integer positionedPageNumber = renderer.<Integer>getProperty(Property.PAGE_NUMBER);
            if (positionedPageNumber == null)
                positionedPageNumber = currentPageNumber;
            renderer.setParent(this).layout(new LayoutContext(new LayoutArea((int) positionedPageNumber, currentArea.getBBox().clone())));

            if (immediateFlush) {
                flushSingleRenderer(renderer);
                positionedRenderers.remove(positionedRenderers.size() - 1);
            }
        }
    }

    // Drawing of content. Might need to rename.
    public void flush() {
        for (IRenderer resultRenderer: childRenderers) {
            flushSingleRenderer(resultRenderer);
        }
        for (IRenderer resultRenderer : positionedRenderers) {
            flushSingleRenderer(resultRenderer);
        }
        childRenderers.clear();
        positionedRenderers.clear();
    }

    /**
     * This method correctly closes the {@link RootRenderer} instance.
     * There might be hanging elements, like in case of {@link Property#KEEP_WITH_NEXT} is set to true
     * and when no consequent element has been added. This method addresses such situations.
     */
    public void close() {
        if (keepWithNextHangingRenderer != null) {
            keepWithNextHangingRenderer.setProperty(Property.KEEP_WITH_NEXT, false);
            IRenderer rendererToBeAdded = keepWithNextHangingRenderer;
            keepWithNextHangingRenderer = null;
            addChild(rendererToBeAdded);
        }
        if (!immediateFlush) {
            flush();
        }
    }

    @Override
    public LayoutResult layout(LayoutContext layoutContext) {
        throw new IllegalStateException("Layout is not supported for root renderers.");
    }

    public LayoutArea getCurrentArea() {
        if (currentArea == null) {
            updateCurrentArea(null);
        }
        return currentArea;
    }

    protected abstract void flushSingleRenderer(IRenderer resultRenderer);

    protected abstract LayoutArea updateCurrentArea(LayoutResult overflowResult);

    private void processRenderer(IRenderer renderer, List<IRenderer> resultRenderers) {
        alignChildHorizontally(renderer, currentArea.getBBox().getWidth());
        if (immediateFlush) {
            flushSingleRenderer(renderer);
        } else {
            resultRenderers.add(renderer);
        }
    }

    private void updateCurrentAreaAndProcessRenderer(IRenderer renderer, List<IRenderer> resultRenderers, LayoutResult result) {
        if (currentArea != null) {
            currentArea.getBBox().setHeight(currentArea.getBBox().getHeight() - result.getOccupiedArea().getBBox().getHeight());
            currentArea.setEmptyArea(false);
            if (renderer != null) {
                processRenderer(renderer, resultRenderers);
            }
        }

        if (!immediateFlush) {
            childRenderers.addAll(resultRenderers);
        }
    }

    private void processWaitingKeepWithNextElement(IRenderer renderer) {
        if (keepWithNextHangingRenderer != null) {
            LayoutArea rest = currentArea.clone();
            rest.getBBox().setHeight(rest.getBBox().getHeight() - keepWithNextHangingRendererLayoutResult.getOccupiedArea().getBBox().getHeight());
            boolean ableToProcessKeepWithNext = false;
            if (renderer.setParent(this).layout(new LayoutContext(rest)).getStatus() != LayoutResult.NOTHING) {
                // The area break will not be introduced and we are safe to place everything as is
                updateCurrentAreaAndProcessRenderer(keepWithNextHangingRenderer, new ArrayList<IRenderer>(), keepWithNextHangingRendererLayoutResult);
                ableToProcessKeepWithNext = true;
            } else {
                float originalElementHeight = keepWithNextHangingRendererLayoutResult.getOccupiedArea().getBBox().getHeight();
                List<Float> trySplitHeightPoints = new ArrayList<>();
                float delta = 35;
                for (int i = 1; i <= 5 && originalElementHeight - delta * i > originalElementHeight / 2; i++) {
                    trySplitHeightPoints.add(originalElementHeight - delta * i);
                }
                for (int i = 0; i < trySplitHeightPoints.size() && !ableToProcessKeepWithNext; i++) {
                    float curElementSplitHeight = trySplitHeightPoints.get(i);
                    LayoutArea firstElementSplitLayoutArea = currentArea.clone();
                    firstElementSplitLayoutArea.getBBox().setHeight(curElementSplitHeight).
                            moveUp(currentArea.getBBox().getHeight() - curElementSplitHeight);
                    LayoutResult firstElementSplitLayoutResult = keepWithNextHangingRenderer.setParent(this).layout(new LayoutContext(firstElementSplitLayoutArea.clone()));
                    if (firstElementSplitLayoutResult.getStatus() == LayoutResult.PARTIAL) {
                        LayoutArea storedArea = currentArea;
                        updateCurrentArea(firstElementSplitLayoutResult);
                        LayoutResult firstElementOverflowLayoutResult = firstElementSplitLayoutResult.getOverflowRenderer().layout(new LayoutContext(currentArea.clone()));
                        if (firstElementOverflowLayoutResult.getStatus() == LayoutResult.FULL) {
                            LayoutArea secondElementLayoutArea = currentArea.clone();
                            secondElementLayoutArea.getBBox().setHeight(secondElementLayoutArea.getBBox().getHeight() - firstElementOverflowLayoutResult.getOccupiedArea().getBBox().getHeight());
                            LayoutResult secondElementLayoutResult = renderer.setParent(this).layout(new LayoutContext(secondElementLayoutArea));
                            if (secondElementLayoutResult.getStatus() != LayoutResult.NOTHING) {
                                ableToProcessKeepWithNext = true;

                                currentArea = firstElementSplitLayoutArea;
                                currentPageNumber = firstElementSplitLayoutArea.getPageNumber();
                                updateCurrentAreaAndProcessRenderer(firstElementSplitLayoutResult.getSplitRenderer(), new ArrayList<IRenderer>(), firstElementSplitLayoutResult);
                                updateCurrentArea(firstElementSplitLayoutResult);
                                updateCurrentAreaAndProcessRenderer(firstElementSplitLayoutResult.getOverflowRenderer(), new ArrayList<IRenderer>(), firstElementOverflowLayoutResult);
                            }
                        }
                        if (!ableToProcessKeepWithNext) {
                            currentArea = storedArea;
                            currentPageNumber = storedArea.getPageNumber();
                        }
                    }
                }
            }
            if (!ableToProcessKeepWithNext && !currentArea.isEmptyArea()) {
                LayoutArea storedArea = currentArea;
                updateCurrentArea(null);
                LayoutResult firstElementLayoutResult = keepWithNextHangingRenderer.setParent(this).layout(new LayoutContext(currentArea.clone()));
                if (firstElementLayoutResult.getStatus() == LayoutResult.FULL) {
                    LayoutArea secondElementLayoutArea = currentArea.clone();
                    secondElementLayoutArea.getBBox().setHeight(secondElementLayoutArea.getBBox().getHeight() - firstElementLayoutResult.getOccupiedArea().getBBox().getHeight());
                    LayoutResult secondElementLayoutResult = renderer.setParent(this).layout(new LayoutContext(secondElementLayoutArea));
                    if (secondElementLayoutResult.getStatus() != LayoutResult.NOTHING) {
                        ableToProcessKeepWithNext = true;
                        updateCurrentAreaAndProcessRenderer(keepWithNextHangingRenderer, new ArrayList<IRenderer>(), keepWithNextHangingRendererLayoutResult);
                    }
                }
                if (!ableToProcessKeepWithNext) {
                    currentArea = storedArea;
                    currentPageNumber = storedArea.getPageNumber();
                }
            }
            if (!ableToProcessKeepWithNext) {
                Logger logger = LoggerFactory.getLogger(RootRenderer.class);
                logger.warn(LogMessageConstant.RENDERER_WAS_NOT_ABLE_TO_PROCESS_KEEP_WITH_NEXT);
                updateCurrentAreaAndProcessRenderer(keepWithNextHangingRenderer, new ArrayList<IRenderer>(), keepWithNextHangingRendererLayoutResult);
            }
            keepWithNextHangingRenderer = null;
            keepWithNextHangingRendererLayoutResult = null;
        }
    }
}
