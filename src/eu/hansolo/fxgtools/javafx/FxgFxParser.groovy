package eu.hansolo.fxgtools.javafx

import groovy.xml.Namespace
import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Ellipse
import javafx.scene.shape.Line
import javafx.scene.shape.Path
import javafx.scene.effect.Effect
import javafx.scene.shape.FillRule
import javafx.scene.shape.MoveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.QuadCurveTo
import javafx.scene.shape.ClosePath
import javafx.scene.text.Text
import javafx.scene.text.Font
import javafx.geometry.VPos
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.Stop
import javafx.scene.paint.RadialGradient
import javafx.geometry.Point2D
import javafx.scene.paint.Paint
import javafx.scene.shape.StrokeType
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.effect.InnerShadow
import javafx.scene.effect.DropShadow
import javafx.scene.transform.Affine
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import javafx.scene.text.TextBoundsType

/**
 * User: han.solo at muenster.de
 * Date: 02.09.11
 * Time: 06:28
 */
class FxgFxParser {
    // Variable declarations
    private final Namespace D = new Namespace("http://ns.adobe.com/fxg/2008/dt")
    private final Namespace FXG = new Namespace("http://ns.adobe.com/fxg/2008")
    private double originalWidth
    private double originalHeight
    private double width
    private double height
    private double scaleFactorX = 1.0
    private double scaleFactorY = 1.0
    private double aspectRatio
    private double offsetX
    private double offsetY
    private double groupOffsetX
    private double groupOffsetY
    private double lastShapeAlpha
    private Affine groupTransform
    private class FxgPathReader {
        protected List path
        protected double scaleFactorX
        protected double scaleFactorY

        FxgPathReader(List newPath, final SCALE_FACTOR_X, final SCALE_FACTOR_Y){
            path = newPath
            scaleFactorX = SCALE_FACTOR_X
            scaleFactorY = SCALE_FACTOR_Y
        }
        String read() {
            path.remove(0)
        }
        double nextX() {
            read().toDouble() * scaleFactorX
        }
        double nextY() {
            read().toDouble() * scaleFactorY
        }
    }


    // ********************   P U B L I C   M E T H O D S   ************************************************************
    Map<String, Group> parse(String fileName, double width, double height, boolean keepAspect) {
        return parse(new XmlParser().parse(new File(fileName)), width, height, keepAspect)
    }

    Map<String, Group> parse(Node fxg, double width, double height, boolean keepAspect) {
        Map<String, Group> groups = [:]
        prepareParameters(fxg, width, height, keepAspect)

        def layers
        if (fxg.Group[0].attribute(D.layerType) && fxg.Group[0].attribute(D.userLabel)) { // fxg contains page attribute
            layers = fxg.Group[0].findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        } else {                                                                          // fxg does not contain page (Fireworks standard)
            layers = fxg.Group.findAll {('layer' == it.attribute(D.type)) && 'false' != it.@visible}
        }

        layers.eachWithIndex {def layer, int i ->
            String layerName = groups.keySet().contains(layer.attribute(D.userLabel)) ? layer.attribute(D.userLabel) : layer.attribute(D.userLabel) + "_$i"
            Group group = new Group()
            groups[layerName] = convertLayer(layer, group)
        }
        return groups
    }


    // ********************   P R I V A T E   M E T H O D S   **********************************************************
    private Rectangle parseRectangle(node) {
        double x = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width = (node.@width ?: 0).toDouble() * scaleFactorX
        double height = (node.@height ?: 0).toDouble() * scaleFactorY
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()
        double radiusX = (node.@radiusX ?: 0).toDouble() * scaleFactorX
        double radiusY = (node.@radiusY ?: 0).toDouble() * scaleFactorY

        Rectangle rect = new Rectangle(x, y, width,height);
        rect.setArcWidth(radiusX > 0 ? radiusX : 0)
        rect.setArcHeight(radiusY > 0 ? radiusY : 0)
        return rect
    }

    private Ellipse parseEllipse(node) {
        double x = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double width = (node.@width ?: 0).toDouble() * scaleFactorX
        double height = (node.@height ?: 0).toDouble() * scaleFactorY
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()

        return new Ellipse(x + width / 2, y + height / 2, width, height)
    }

    private Line parseLine(node) {
        double xFrom = ((node.@xFrom ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yFrom = ((node.@yFrom ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double xTo = ((node.@xTo ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double yTo = ((node.@yTo ?: 0).toDouble() + groupOffsetY) * scaleFactorX
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()

        return new Line(xFrom, yFrom, xTo, yTo)
    }

    private Path parsePath(node) {
        String data = node.@data ?: ''
        double x = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        //double scaleX = (node.@scaleX ?: 0).toDouble()
        //double scaleY = (node.@scaleY ?: 0).toDouble()
        //double rotation = (node.@rotation ?: 0).toDouble()
        lastShapeAlpha = (node.@alpha ?: 1).toDouble()
        String winding = (node.@winding ?: 'evenOdd')
        Path path = new Path()
        path.setFillRule(winding == 'evenOdd' ? FillRule.EVEN_ODD : FillRule.NON_ZERO)

        data = data.replaceAll(/([A-Za-z])/, / $1 /) // wrap single characters in blanks
        def pathList = data.tokenize()
        def pathReader = new FxgPathReader(pathList, scaleFactorX, scaleFactorY)

        processPath(pathList, pathReader, path, x, y)

        return path
    }

    private processPath(pathList, FxgPathReader reader, Path path, double x, double y) {
        while (pathList) {
            switch (reader.read()) {
                case "M":
                    path.getElements().add(new MoveTo(reader.nextX() + x, reader.nextY() + y))
                    break
                case "L":
                    path.getElements().add(new LineTo(reader.nextX() + x, reader.nextY() + y))
                    break
                case "C":
                    path.getElements().add(new CubicCurveTo(reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y))
                    break
                case "Q":
                    path.getElements().add(new QuadCurveTo(reader.nextX() + x, reader.nextY() + y, reader.nextX() + x, reader.nextY() + y))
                    break
                case "Z":
                    path.getElements().add(new ClosePath())
                    break
            }
        }
    }

    private Text parseRichText(node) {
        def fxgLabel = node.content[0].p[0]
        String text
        double fontSize
        String colorString
        if (fxgLabel.span) {
            // Adobe Illustrator
            text = node.content[0].p[0].span[0].text()
            fontSize = (node.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (node.@color ?: '#000000')
        } else {
            // Adobe Fireworks
            text = fxgLabel.text()
            fontSize = (fxgLabel.@fontSize ?: 10).toDouble() * scaleFactorX
            colorString = (node.content.p.@color[0] ?: '#000000')
        }
        double x = ((node.@x ?: 0).toDouble() + groupOffsetX) * scaleFactorX
        double y = ((node.@y ?: 0).toDouble() + groupOffsetY) * scaleFactorY
        double rotation = ((node.@rotation ?: 0).toDouble())
        double scaleX = ((node.@scaleX ?: 1).toDouble())
        double scaleY = ((node.@scaleY ?: 1).toDouble())
        String fontFamily = (fxgLabel.@fontFamily ?: 'sans-serif')
        String fontStyle = (node.@fontStyle ?: 'normal')
        String textDecoration = (node.@textDecoration ?: 'none')
        boolean lineThrough = ((node.@lineThrough ?: 'false')) == 'true'
        double alpha = (node.@alpha ?: 1).toDouble() * lastShapeAlpha
        y += fontSize
        Color color = parseColor(colorString, alpha)
        boolean bold = ((fxgLabel.@fontWeight ?: 'normal') == 'bold') == 'bold'
        boolean italic = fontStyle == 'italic'
        boolean underline = textDecoration == 'underline'
        FontWeight fontWeight = (bold ? FontWeight.BOLD : FontWeight.NORMAL)
        FontPosture fontPosture = (italic ? FontPosture.ITALIC : FontPosture.REGULAR)
        Text richtext = new Text(text.trim())
        richtext.setFont(Font.font(fontFamily, fontWeight, fontPosture, fontSize))
        richtext.setX(x)
        richtext.setY(y)
        richtext.setTextOrigin(VPos.BOTTOM)
        richtext.setStrikethrough(lineThrough)
        richtext.setUnderline(underline)
        richtext.setFill(color)
        //richtext.boundsType = TextBoundsType.LOGICAL
        richtext.getTransforms().add(new Rotate(rotation, richtext.x, richtext.y))
        richtext.getTransforms().add(new Scale(scaleX, scaleY))
        if (node.transform) {
            richtext.transforms.add(parseTransform(node))
        }
        return richtext
    }

    private Paint parseFill(node) {
        Paint paint = null
        if (node.fill) {
            def fill = node.fill[0]
            if (fill != null) {
                if (fill.SolidColor) {
                    paint = parseColor(node.fill.SolidColor[0])
                }
                if (fill.LinearGradient){
                    paint = convertLinearGradient(node.fill)
                }
                if (fill.RadialGradient) {
                    paint = convertRadialGradient(node.fill)
                }
            }
        }
        return paint
    }

    private Shape parseStroke(node, shape) {
        if (node.stroke) {
            def stroke = node.stroke
            if (stroke.SolidColorStroke) {
                def solidColorStroke = stroke[0].SolidColorStroke
                String colorString = (solidColorStroke[0].@color ?: '#000000')
                double weight = (solidColorStroke[0].@weight ?: 1f).toDouble() * scaleFactorX
                String caps = (solidColorStroke[0].@caps ?: 'round')
                String joints = (solidColorStroke[0].@joints ?: 'round')
                int alpha = (solidColorStroke[0].@alpha ?: 1).toDouble() * lastShapeAlpha
                Color color = parseColor(colorString, alpha)

                weight.compareTo(2.0) <= 0 ? shape.setStrokeType(StrokeType.OUTSIDE) : shape.setStrokeType(StrokeType.CENTERED)

                switch (caps) {
                    case 'none':
                        shape.setStrokeLineCap(StrokeLineCap.BUTT)
                        break
                    case 'round':
                        shape.setStrokeLineCap(StrokeLineCap.ROUND)
                        break
                    case 'square':
                        shape.setStrokeLineCap(StrokeLineCap.SQUARE)
                        break
                }
                switch (joints) {
                    case 'bevel':
                        shape.setStrokeLineJoin(StrokeLineJoin.BEVEL)
                        break
                    case 'round':
                        shape.setStrokeLineJoin(StrokeLineJoin.ROUND)
                        break
                    case 'mite':
                        shape.setStrokeLineJoin(StrokeLineJoin.MITER)
                        break
                }
                shape.setStrokeWidth(weight)
                shape.setStroke(color)
            }
        } else {
            shape.setStroke(null)
        }

        return shape
    }

    private Affine parseTransform(node) {
        Affine transform = new Affine()
        if (node.transform.Transform.matrix.Matrix) {
            def matrix = node.transform.Transform.matrix.Matrix
            transform.setMxx((matrix.@a[0] ?: 0.0).toDouble()) // scaleX
            transform.setMyx((matrix.@b[0] ?: 0.0).toDouble()) // shearY
            transform.setMxy((matrix.@c[0] ?: 0.0).toDouble()) // shearX
            transform.setMyy((matrix.@d[0] ?: 0.0).toDouble()) // scaleY
            transform.setTx(((matrix.@tx[0] ?: 0.0).toDouble() + groupOffsetX) * scaleFactorX) // translateX
            transform.setTy(((matrix.@ty[0] ?: 0.0).toDouble() + groupOffsetY) * scaleFactorY) // translateY
        }
        return transform
    }

    private Effect parseFilter(node, shape) {
        Effect shapeFilters = null
        if (node.filters) {
            node.filters.eachWithIndex { filter, i ->
                if (filter.DropShadowFilter) {
                    filter.DropShadowFilter.each { Node dropShadow ->
                        if (shapeFilters != null) {
                            shapeFilters.inputProperty().set(addFilter(dropShadow, shapeFilters))
                        } else {
                            shapeFilters = addFilter(dropShadow, shapeFilters)
                        }
                    }
                }
            }
        }
        return shapeFilters
    }

    private Effect addFilter(Node dropShadow, Effect fxgFilter) {
        double angle = Math.toRadians(-(dropShadow.@angle ?: 0).toDouble())
        String colorString = (dropShadow.@color ?: '#000000')
        int distance = (dropShadow.@distance ?: 0).toDouble()
        double alpha = (dropShadow.@alpha ?: 1).toDouble() * lastShapeAlpha
        int blurX = (dropShadow.@blurX ?: 0).toDouble() * scaleFactorX
        //int blurY = (filter.DropShadowFilter.@blurY ?: 0).toDouble() * scaleFactorY
        boolean inner = (dropShadow.@inner ?: false)
        Color color = parseColor(colorString, alpha)
        double offsetX = distance * Math.cos(angle) * scaleFactorX
        double offsetY = distance * Math.sin(angle) * scaleFactorY

        if (inner) {
            fxgFilter = new InnerShadow(blurX, offsetX, offsetY, color)
        } else {
            DropShadow dShadow = new DropShadow()
            dShadow.setOffsetX(offsetX)
            dShadow.setOffsetY(offsetY)
            dShadow.setRadius(blurX)
            dShadow.setColor(color)
            fxgFilter = dShadow
        }
        return fxgFilter
    }

    private Color parseColor(node) {
        String color = (node.@color ?: '#000000')
        double alpha = (node.@alpha ?: 1).toDouble() * lastShapeAlpha
        return parseColor(color, alpha)
    }

    private Color parseColor(String color, double alpha) {
        assert color.size() == 7
        double red = Integer.valueOf(color[1..2], 16).intValue() / 255
        double green = Integer.valueOf(color[3..4], 16).intValue() /255
        double blue = Integer.valueOf(color[5..6], 16).intValue() / 255
        return new Color(red, green, blue, alpha)
    }

    private convertSolidColor(paint, node) {
        paint.color = parseColor((node.SolidColor[0] ?: '#000000'))
    }

    private Paint convertLinearGradient(node) {
        def linearGradient = node.LinearGradient[0]
        double x1 = (linearGradient.@x ?: 0).toDouble() * scaleFactorX
        double y1 = (linearGradient.@y ?: 0).toDouble() * scaleFactorY
        double scaleX = (linearGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (linearGradient.@scaleY ?: 1).toDouble()
        double rotation = Math.toRadians((linearGradient.@rotation ?: 0).toDouble())
        double x2 = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2 = Math.sin(rotation) * scaleX * scaleFactorY + y1
        def gradientEntries = linearGradient.GradientEntry
        Stop[] stops = convertGradientEntries(gradientEntries)

        LinearGradient gradient = new LinearGradient(x1 + offsetX, y1 + offsetY, x2 + offsetX, y2 + offsetY, false, CycleMethod.NO_CYCLE, stops)

        return gradient
    }

    private Paint convertRadialGradient(node) {
        def radialGradient = node.RadialGradient[0]
        double x1 = (radialGradient.@x ?: 0).toDouble() * scaleFactorX
        double y1 = (radialGradient.@y ?: 0).toDouble() * scaleFactorY
        double scaleX = (radialGradient.@scaleX ?: 0).toDouble()
        //double scaleY = (radialGradient.@scaleY ?: 0).toDouble()
        double rotation = Math.toRadians((radialGradient.@rotation ?: 0).toDouble())
        double x2 = Math.cos(rotation) * scaleX * scaleFactorX + x1
        double y2 = Math.sin(rotation) * scaleX * scaleFactorY + y1
        Point2D center = new Point2D(x1, y1)
        Point2D stop = new Point2D(x2, y2)
        double radius = (center.distance(stop) / 2.0)
        def gradientEntries = radialGradient.GradientEntry

        Stop[] stops = convertGradientEntries(gradientEntries)
        RadialGradient gradient = new RadialGradient(0, 0, center.x + offsetX, center.y + offsetY, radius, false, CycleMethod.NO_CYCLE, stops)

        return gradient
    }

    private Stop[] convertGradientEntries(gradientEntries) {
        List stops = []
        gradientEntries.each { def gradientEntry->
            double fraction = (gradientEntry.@ratio ?: 0).toDouble()
            double alpha = (gradientEntry.@alpha ?: 1).toDouble() * lastShapeAlpha
            Color color = gradientEntry.@color == null ? Color.BLACK : parseColor(gradientEntry.@color, alpha)
            stops.add(new Stop(fraction, color))
        }
        return stops
    }

    private Shape paintShape(node, shape) {
        if (groupTransform != null) {
            shape.transforms.add(groupTransform)
        }
        shape.setFill(parseFill(node))
        parseStroke(node, shape)
        if(node.filters) {
            shape.setEffect(parseFilter(node, shape))
        }
        groupTransform = null
        return shape
    }

    private Group convertLayer(layer, group) {
        layer.each {Node node->
            Shape shape
            switch(node.name()) {
                case FXG.Group:
                    groupOffsetX = (node.@x ?: 0).toDouble()
                    groupOffsetY = (node.@y ?: 0).toDouble()
                    if (node.transform) {
                        groupTransform = parseTransform(node)
                    } else {
                        groupTransform = null
                    }
                    convertLayer(node, group)
                    break
                case FXG.Rect:
                    shape = parseRectangle(node)
                    offsetX = shape.layoutBounds.minX
                    offsetY = shape.layoutBounds.minY
                    group.getChildren().add(paintShape(node, shape))
                    break
                case FXG.Ellipse:
                    shape = parseEllipse(node)
                    offsetX = shape.layoutBounds.minX
                    offsetY = shape.layoutBounds.minY
                    group.getChildren().add(paintShape(node, shape))
                    break
                case FXG.Line:
                    shape = parseLine(node)
                    offsetX = shape.layoutBounds.minX
                    offsetY = shape.layoutBounds.minY
                    group.getChildren().add(paintShape(node, shape))
                    break
                case FXG.Path:
                    offsetX = groupOffsetX
                    offsetY = groupOffsetY
                    shape = parsePath(node)
                    group.getChildren().add(paintShape(node, shape))
                    break
                case FXG.RichText:
                    Text text = parseRichText(node)
                    group.getChildren().add(text)
                    break
            }
        }
        return group
    }

    private void prepareParameters(def fxg, final double WIDTH, final double HEIGHT, final boolean KEEP_ASPECT) {
        originalWidth = (int)(fxg.@viewWidth ?: 100).toDouble()
        originalHeight = (int)(fxg.@viewHeight ?: 100).toDouble()

        width = WIDTH
        height = KEEP_ASPECT ? WIDTH * (originalHeight / originalWidth) : HEIGHT

        aspectRatio = originalHeight / originalWidth

        scaleFactorX = width / originalWidth
        scaleFactorY = height / originalHeight
    }
}
