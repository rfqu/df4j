/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.example;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.github.rfqu.df4j.util.BinaryOp;
import com.github.rfqu.df4j.core.FuturePort;
import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.util.UnaryOp;

public class NodeTest {
    
    public NodeTest() {
        Task.setCurrentExecutor(new SimpleExecutorService());
    }

    class Square extends UnaryOp<Integer, Integer> {
        public Integer operation(Integer v) {
            return v * v;
        }
    }

    class Sqrt extends UnaryOp<Integer, Double> {
        public Double operation(Integer v) {
            return Math.sqrt(v.doubleValue());
        }
    }

    class Sum extends BinaryOp<Integer, Integer> {
        public Integer operation(Integer v1, Integer v2) {
            return v1 + v2;
        }
    }

    class Mult extends BinaryOp<Integer, Integer> {
        public Integer operation(Integer v1, Integer v2) {
            return v1 * v2;
        }
    }

    class Div extends BinaryOp<Integer, Integer> {
        public Integer operation(Integer v1, Integer v2) {
            if (v2==0) {
                
            }
            return v1 / v2;
        }
    }

    /**
     * compute a^2
     * 
     * @throws InterruptedException
     */
    @Test
    public void t01() throws InterruptedException {
        FuturePort<Integer> future = new FuturePort<Integer>();
        new Square().send(2).res.request(future);
        int res = future.get();
        assertEquals(4, res);
    }

    /**
     * compute 2*3
     */
    @Test
    public void t02() throws InterruptedException {
        FuturePort<Integer> future = new FuturePort<Integer>();
        new Mult().p1.send(2).p2.send(3).request(future);
        int res = future.get();
        assertEquals(6, res);
    }

    /**
     * compute sqrt(a^2+b^2)
     */
    @Test
    public void t03() throws InterruptedException {
        FuturePort<Double> future = new FuturePort<Double>();
        Sum sum = new Sum();
        new Square().send(3).request(sum.p1);
        new Square().send(4).request(sum.p2);
        sum.request(new Sqrt()).request(future);
        double res = future.get();
        assertEquals(5, res, 0.00001);
    }
    
    public static void main(String args[]) throws InterruptedException {
        NodeTest t=new NodeTest();
        t.t01();
        t.t02();
        t.t03();
    }
}