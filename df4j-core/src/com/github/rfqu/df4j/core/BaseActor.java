/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

/**
 * Abstract dataflow node with several inputs and outputs.
 * Typical use is:
 *  - create 1 or more pins for inputs and/or outputs
 *  - redefine abstract methods removeTokens() and act()
 */
public abstract class BaseActor extends Task {
	static final int allOnes=0xFFFFFFFF;
    private int pinCount=0;
    private int pinMask=0; // mask with 1 for all existing pins
    private int readyPins=0;  // mask with 1 for all ready pins
    protected boolean fired=false; // true when this actor runs
    
    public BaseActor(Executor executor) {
    	super(executor);
    }

    public BaseActor() {
    }

    /**
     * @return true if the actor has all its pins on and so is ready for execution
     */
    private final boolean isReady() {
		return readyPins==pinMask;
	}

    /** 
     * Removes tokens from pins.
     * Removed tokens are to be saved and then used in the act() method.
     * Should remove at least 1 token to avoid infinite loop.
     * Should return quickly, as is called from synchronized block.
     */
    protected abstract void removeTokens();

    /** 
     * process the retrieved tokens.
     */
    protected abstract void act();

    /** loops while all pins are ready
     */
    @Override
    public void run() {
        for (;;) {
            synchronized (this) {
                if (!isReady()) {
                    fired=false; // allow firing
                    return;
                }
                removeTokens();
            }
            act();
        }
    }
    
    /**
     * Like a place in Petri net.
     * Can be turned on or off. 
     * Initial state should be off, to prevent premature firing.
     */
    protected abstract class Pin {
    	private final int pinBit;

    	Pin(){
        	synchronized (BaseActor.this) {
            	int count = pinCount;
                if (count==32) {
              	  throw new IllegalStateException("only 32 pins could be created");
                }
                pinCount++;
                pinBit = 1<<count;
            	pinMask=pinMask|pinBit;
			}
        }

        protected void turnOn() {
        	synchronized (BaseActor.this) {
                readyPins |= pinBit;
                if (fired || !isReady()) {
                    return;
                }
                fired = true; // to prevent multiple concurrent firings
            }
            fire();
        }

        protected void turnOff() {
            synchronized (BaseActor.this) {
                readyPins &= ~pinBit;
            }
        }

    }

    /**
     *  Only stops/allows actor execution
     */
    protected class BooleanPlace extends Pin {
    	private boolean on=false;
    	
		public BooleanPlace() { }

        public void send() {
            synchronized (BaseActor.this) {
            	if (on) {
    				throw new IllegalStateException("place is occupied already"); 
            	}
            	on=true;
            	turnOn();
            }
        }

        public void remove() {
            synchronized (BaseActor.this) {
            	if (!on) {
    				throw new IllegalStateException("place is not occupied"); 
            	}
            	on=false;
            	turnOff();
            }
        }

		protected boolean isEmpty() {
			return !on;
		}
    }

    /** 
     * holds tokens without data 
     */
    protected class PetriPlace extends Pin {
    	private int count=0;
    	
        public PetriPlace() { }

        public void send(int delta) {
            synchronized (BaseActor.this) {
                if (delta<0) {
                    throw new IllegalArgumentException("PetriPlace.send: delta<0");
                }
                if (delta==0) {
                    return;
                }
                boolean doTurn=(count==0);
            	count+=delta;
            	if (doTurn) {
            		turnOn();
            	}
            }
        }

        public void remove() {
            synchronized (BaseActor.this) {
            	if (count==0) {
    				throw new IllegalStateException("place is empty"); 
            	}
            	count--;
            	if (count==0) {
                	turnOff();
            	}
            }
        }

		protected boolean isEmpty() {
			return count==0;
		}
    }

    protected abstract class BasePort<T> extends Pin implements Port<T> {
        public T token=null;

        @Override
        public void send(T token) {
            synchronized (BaseActor.this) {
            	add(token);
            	turnOn();
            }
        }

        public T retrieve() {
            synchronized (BaseActor.this) {
                token =_remove();
            	if (isEmpty()) {
                	turnOff();
            	}
            	return token;
            }
        }

		protected abstract void add(T newToken);
		protected abstract boolean isEmpty();
		protected abstract T _remove();
    }

    /** A place for single token loaded with a reference of type <T>
     */
    public class ScalarInput<T> extends BasePort<T> {
        protected T operand=null;

        public ScalarInput() { }

		@Override
		protected void add(T newToken) {
			if (newToken==null) {
				throw new IllegalArgumentException("operand may not be null"); 
			}
			if (operand!=null) {
				throw new IllegalStateException("place is occupied already"); 
			}
			operand=newToken;
		}

		@Override
		protected boolean isEmpty() {
			return operand==null;
		}

		@Override
		protected T _remove() {
	        if (isEmpty() ) {
	            throw new NoSuchElementException();
	        }
	        T res=operand;
			operand=null;
			return res;
		}

		public T get() {
			return operand;
		}
    }

    /** A Queue of tokens of type <T>
     */
    public class StreamInput<T extends Link> extends BasePort<T> implements StreamPort<T>{
    	private LinkedQueue<T> queue=new LinkedQueue<T>();
    	private boolean closeRequested=false;
    	private boolean closeHandled=false;

        public StreamInput() { }

		@Override
		protected void add(T token) {
			if (token==null) {
				throw new IllegalArgumentException("operand may not be null"); 
			}
			queue.add(token);
		}

		@Override
        public boolean isEmpty() {
			if (!queue.isEmpty()) {
				return false;
			}
			return closeRequested==closeHandled;
		}

		@Override
		protected T _remove() {
			T res = queue.poll();
			if (res!=null) {
				return res;
			}
			if (closeRequested) {
				closeHandled=true;
			}
			return null;
		}

		public boolean isClosed() {
			return closeRequested;
		}

		/** Signals the end of the stream. 
		 * Turns this pin on. Removed value is null 
		 * (null cannot be send with StreamInput.add(message)).
		 */
		@Override
		public void close() {
            synchronized (BaseActor.this) {
    			closeRequested=true;
            	turnOn();
            }
		}
    }

    /**
     * 
     * This pin carries demand(s) of the result.
     * @param <R>  type of result
     */
    public class Demand<R> extends Pin implements Port<R>{
        private Connector<R> connector=new Connector<R>();

        /** indicates a demand
         */
        public void connect(Port<R> sink) {
            synchronized (BaseActor.this) {
            	connector.connect(sink);
            	turnOn();
            }
    	}

    	public void connect(StreamPort<R> sink) {
            synchronized (BaseActor.this) {
                synchronized (BaseActor.this) {
                	connector.connect(sink);
                	turnOn();
                }
            }
    	}

    	public void connect(Port<R>... sinks) {
            synchronized (BaseActor.this) {
            	connector.connect(sinks);
            	turnOn();
            }
    	}

    	/** satisfy demand(s)
    	 */
    	@Override
		public void send(R m) {
			connector.send(m);
		}

    }
    

}