package com.itextpdf.kernel.geom;

import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.UnitTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnitTest.class)
public class VectorTest extends ExtendedITextTest {

    @Test
    public void testCrossVector() {
        Vector v = new Vector(2, 3, 4);
        Matrix m = new Matrix(5, 6, 7, 8, 9, 10);
        Vector shouldBe = new Vector(67, 76, 4);

        Vector rslt = v.cross(m);
        Assert.assertEquals(shouldBe, rslt);
    }

}
