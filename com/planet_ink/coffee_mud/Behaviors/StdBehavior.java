package com.planet_ink.coffee_mud.Behaviors;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;

import java.util.*;

/**
 * something that is affected by, or affects
 * the environment around them.
 */
public class StdBehavior implements Behavior
{
	public String ID(){return "StdBehavior";}
	public String name(){return ID();}
	protected int canImproveCode(){return Behavior.CAN_MOBS;}
	public boolean grantsMobility(){return false;}
	public boolean grantsAggressivenessTo(MOB M){return false;}
	
	protected String parms="";

	/** return a new instance of the object*/
	public Behavior newInstance()
	{
		return new StdBehavior();
	}
	public Behavior copyOf()
	{
		try
		{
			return (Behavior)this.clone();
		}
		catch(CloneNotSupportedException e)
		{
			return new StdBehavior();
		}
	}
	public void startBehavior(Environmental forMe)
	{

	}
	public boolean modifyBehavior(MOB mob, Object O)
	{ return false; }
	protected MOB getBehaversMOB(Tickable ticking)
	{
		if(ticking==null) return null;

		if(ticking instanceof MOB)
			return (MOB)ticking;
		else
		if(ticking instanceof Item)
			if(((Item)ticking).owner() != null)
				if(((Item)ticking).owner() instanceof MOB)
					return (MOB)((Item)ticking).owner();

		return null;
	}

	protected Room getBehaversRoom(Tickable ticking)
	{
		if(ticking==null) return null;

		if(ticking instanceof Room)
			return (Room)ticking;

		MOB mob=getBehaversMOB(ticking);
		if(mob!=null)
			return mob.location();

		if(ticking instanceof Item)
			if(((Item)ticking).owner() != null)
				if(((Item)ticking).owner() instanceof Room)
					return (Room)((Item)ticking).owner();

		return null;
	}

	public String getParms(){return parms;}
	public void setParms(String parameters){parms=parameters;}
	public int compareTo(Object o){ return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));}

	/** this method defines how this thing responds
	 * to environmental changes.  It may handle any
	 * and every affect listed in the Affect class
	 * from the given Environmental source */
	public void affect(Environmental affecting, Affect affect)
	{
		return;
	}

	/** this method is used to tell the system whether
	 * a PENDING affect may take place
	 */
	public boolean okAffect(Environmental oking, Affect affect)
	{
		return true;
	}

	protected int getParmVal(String text, String key, int defaultValue)
	{
		text=text.toUpperCase();
		key=key.toUpperCase();
		int x=text.indexOf(key);
		while(x>=0)
		{
			if((x==0)||(!Character.isLetter(text.charAt(x-1))))
			{
				while((x<text.length())&&(text.charAt(x)!='=')&&(!Character.isDigit(text.charAt(x))))
					x++;
				if((x<text.length())&&(text.charAt(x)=='='))
				{
					while((x<text.length())&&(!Character.isDigit(text.charAt(x))))
						x++;
					if(x<text.length())
					{
						text=text.substring(x);
						x=0;
						while((x<text.length())&&(Character.isDigit(text.charAt(x))))
							x++;
						return Util.s_int(text.substring(0,x));
					}
				}
				x=-1;
			}
			else
				x=text.toUpperCase().indexOf(key.toUpperCase(),x+1);
		}
		return defaultValue;
	}
	
	public boolean canImprove(Environmental E)
	{
		if((E==null)&&(canImproveCode()==0)) return true;
		if(E==null) return false;
		if((E instanceof MOB)&&((canImproveCode()&Ability.CAN_MOBS)>0)) return true;
		if((E instanceof Item)&&((canImproveCode()&Ability.CAN_ITEMS)>0)) return true;
		if((E instanceof Exit)&&((canImproveCode()&Ability.CAN_EXITS)>0)) return true;
		if((E instanceof Room)&&((canImproveCode()&Ability.CAN_ROOMS)>0)) return true;
		if((E instanceof Area)&&((canImproveCode()&Ability.CAN_AREAS)>0)) return true;
		return false;
	}
	public static boolean canActAtAll(Tickable affecting)
	{
		if(affecting==null) return false;
		if(!(affecting instanceof MOB)) return false;

		MOB monster=(MOB)affecting;
		if(monster.amDead()) return false;
		if(monster.location()==null) return false;
		if(!Sense.aliveAwakeMobile(monster,true)) return false;
		return true;
	}

	public static boolean canFreelyBehaveNormal(Tickable affecting)
	{
		if(affecting==null) return false;
		if(!(affecting instanceof MOB)) return false;

		MOB monster=(MOB)affecting;
		if(!canActAtAll(monster))
			return false;
		if(monster.isInCombat()) return false;
		if(monster.amFollowing()!=null)  return false;
		if(monster.curState().getHitPoints()<((int)Math.round(monster.maxState().getHitPoints()/2.0)))
			return false;
		return true;
	}

	/**
	 * this method allows any environmental object
	 * to behave according to a timed response.  by
	 * default, it will never be called unless the
	 * object uses the ServiceEngine to setup service.
	 * The tickID allows granularity with the type
	 * of service being requested.
	 */
	public boolean tick(Tickable ticking, int tickID)
	{
		return true;
	}
	private static final String[] CODES={"CLASS","TEXT"};
	public String[] getStatCodes(){return CODES;}
	private int getCodeNum(String code){
		for(int i=0;i<CODES.length;i++)
			if(code.equalsIgnoreCase(CODES[i])) return i;
		return -1;
	}
	public String getStat(String code){
		switch(getCodeNum(code))
		{
		case 0: return ID();
		case 1: return getParms();
		}
		return "";
	}
	public void setStat(String code, String val)
	{
		switch(getCodeNum(code))
		{
		case 0: return;
		case 1: setParms(val); break;
		}
	}
	public boolean sameAs(Behavior E)
	{
		if(!(E instanceof StdBehavior)) return false;
		for(int i=0;i<CODES.length;i++)
			if(!E.getStat(CODES[i]).equals(getStat(CODES[i])))
				return false;
		return true;
	}
}
