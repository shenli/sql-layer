/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.expression.std;

import com.foundationdb.junit.NamedParameterizedRunner;
import com.foundationdb.junit.NamedParameterizedRunner.TestParameters;
import com.foundationdb.junit.Parameterization;
import com.foundationdb.junit.ParameterizationBuilder;
import com.foundationdb.server.expression.Expression;
import com.foundationdb.server.expression.ExpressionComposer;
import com.foundationdb.server.types.AkType;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(NamedParameterizedRunner.class)
public class ConvExpressionTest extends ComposedExpressionTestBase
{
    private static boolean alreadyExc = false;

    private String num;
    private Long from;
    private Long to;
    private String expected;
    
    public ConvExpressionTest (String num, Long from, Long to, String expected)
    {
        this.num = num;
        this.from = from;
        this.to = to;
        this.expected = expected;
    }
    
    @TestParameters
    public static Collection<Parameterization> params()
    {
        ParameterizationBuilder p = new ParameterizationBuilder();
        
        String dec = "255";
        String hex = "FF";
        String bin = "11111111";
        
        param(p, hex, 16L, 10L, dec);
        param(p, hex, 16L, 2L, bin);
        param(p, bin, 2L, 10L, dec);
        param(p, bin, 2L, 16L, hex);
        param(p, dec, 10L, 16L, hex);
        param(p, dec, 10L, 2L, bin);
        
        // test convert to same base
        String val;
        param(p, val = "A", 16L, 16L, val);
        param(p, val = "123231231", 15L, 15L, val);
        param(p, "1234", 2L, 2L, "0");
        
        // test error
        param(p, hex, 10L, 2L, "0");
        param(p, "123", 56L, 3L, null);
        param(p, "10", 2L, 1L, null);
        param(p, "0", 1L, 0L, null);
        
        // test signed
        param(p, "-1", 10L, 16L, "FFFFFFFFFFFFFFFF");
        return p.asList();
    }
    
    private static void param(ParameterizationBuilder p, String num, Long from, Long to, String exp)
    {
        p.add("CONV(" + num + ", " + from + ", " + to + ") ", num, from, to, exp);
    }
    
    private static Expression getExp(Object val)
    {
        return val == null 
                ? LiteralExpression.forNull()
                : val instanceof Long
                    ? new LiteralExpression(AkType.LONG, ((Long)val).longValue())
                    : new LiteralExpression (AkType.VARCHAR, (String)val);
    }
    
    @Test
    public void test()
    {
        alreadyExc = true;
        Expression top = new ConvExpression(Arrays.asList(getExp(num),
                                                          getExp(from),
                                                          getExp(to)));
        
        if (expected == null)
            assertTrue("Top should be NULL ", top.evaluation().eval().isNull());
        else
            assertEquals(expected, top.evaluation().eval().getString());
    }
    
    @Override
    protected CompositionTestInfo getTestInfo()
    {
        return new CompositionTestInfo(3, AkType.VARCHAR, true);
    }

    @Override
    protected ExpressionComposer getComposer()
    {
        return ConvExpression.COMPOSER;
    }

    @Override
    public boolean alreadyExc()
    {
        return alreadyExc;
    }
}