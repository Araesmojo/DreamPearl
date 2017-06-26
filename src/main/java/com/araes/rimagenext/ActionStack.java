package com.araes.rimagenext;

import java.util.List;
import java.util.ArrayList;

public class ActionStack 

{
	private List<BBoxAction> stack;

	public ActionStack() 
	{
		stack = new ArrayList<BBoxAction>();
	}

	public void push(BBoxAction act) 
	{
		stack.add(0,act);
	}
	
	public int length()
	{
		return stack.size();
	}

	public BBoxAction pop() 
	{ 
		if(!stack.isEmpty()){
			BBoxAction act= stack.get(0);
			stack.remove(0);
			return act;
		} else{
			return null;// Or any invalid value
		}
	}

	public BBoxAction peek()
	{
		if(!stack.isEmpty()){
			return stack.get(0);
		} else{
			return null;// Or any invalid value
		}
	}

	public void clear(){
		stack.clear();
	}

	public boolean isEmpty() 
	{
		return stack.isEmpty();
	}
}	
