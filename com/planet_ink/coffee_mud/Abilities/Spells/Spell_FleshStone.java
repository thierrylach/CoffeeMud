package com.planet_ink.coffee_mud.Abilities.Spells;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import com.planet_ink.coffee_mud.Abilities.Spells.interfaces.*;
import java.util.*;

public class Spell_FleshStone extends Spell
	implements AlterationDevotion
{

	String previousDisplayText="";

	public Spell_FleshStone()
	{
		super();
		myID=this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
		name="Flesh Stone";

		// what the affected mob sees when they
		// bring up their affected list.
		displayText="(Flesh to Stone)";


		quality=Ability.MALICIOUS;

		canBeUninvoked=true;
		isAutoinvoked=false;

		baseEnvStats().setLevel(19);

		addQualifyingClass("Mage",19);
		addQualifyingClass("Ranger",baseEnvStats().level()+4);

		uses=Integer.MAX_VALUE;
		recoverEnvStats();
	}

	public Environmental newInstance()
	{
		return new Spell_FleshStone();
	}

	public boolean okAffect(Affect affect)
	{
		if((affected==null)||(!(affected instanceof MOB)))
			return true;

		MOB mob=(MOB)affected;
		if(affect.source().getVictim()==mob)
			affect.source().setVictim(null);
		if(mob.isInCombat()) mob.makePeace();
		mob.recoverMaxState();
		mob.resetToMaxState();
		mob.curState().setHunger(1000);
		mob.curState().setThirst(1000);
		mob.recoverCharStats();
		mob.recoverEnvStats();

		// when this spell is on a MOBs Affected list,
		// it should consistantly prevent the mob
		// from trying to do ANYTHING except sleep
		if(affect.amISource(mob))
		{
			if((!Util.bset(affect.sourceMajor(),Affect.ACT_GENERAL))
			&&(affect.sourceMajor()>0))
			{
				mob.tell("Statues can't do that.");
				return false;
			}
		}
		if(affect.amITarget(mob))
		{
			if((affect.targetMinor()==Affect.TYP_CAST_SPELL)
			   &&(affect.tool()!=null)
			   &&(affect.tool() instanceof Spell_StoneFlesh))
			{
				affect.source().location().show(mob,null,Affect.MSG_OK_VISUAL,"<S-NAME> seem(s) to smile.");
			}
			else
			{
				Item item=CMClass.getItem("GenItem");
				item.setName(mob.name());
				item.setDescription(mob.description());
				item.setDisplayText(mob.displayText());
				item.setGettable(false);
				item.envStats().setWeight(2000);
				FullMsg msg=new FullMsg(affect.source(),item,affect.targetCode(),null);
				if(!okAffect(msg))
					return false;
			}
		}
		if(!super.okAffect(affect))
			return false;
		
		if(affect.source().getVictim()==mob)
			affect.source().setVictim(null);
		if(mob.isInCombat()) mob.makePeace();
		return true;
	}

	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		super.affectEnvStats(affected,affectableStats);
		// when this spell is on a MOBs Affected list,
		// it should consistantly put the mob into
		// a sleeping state, so that nothing they do
		// can get them out of it.
		affectableStats.setSensesMask(affectableStats.sensesMask()|Sense.CAN_MOVE);
		affectableStats.setSensesMask(affectableStats.sensesMask()|Sense.CAN_HEAR);
		affectableStats.setSensesMask(affectableStats.sensesMask()|Sense.CAN_SMELL);
		affectableStats.setSensesMask(affectableStats.sensesMask()|Sense.CAN_SPEAK);
		affectableStats.setSensesMask(affectableStats.sensesMask()|Sense.CAN_TASTE);
	}


	public void unInvoke()
	{
		// undo the affects of this spell
		if((affected==null)||(!(affected instanceof MOB)))
			return;
		MOB mob=(MOB)affected;

		super.unInvoke();
		mob.tell("Your flesh returns to normal!");
		mob.curState().setHitPoints(1);
		mob.curState().setMana(0);
		mob.curState().setMovement(0);
		mob.curState().setHunger(0);
		mob.curState().setThirst(0);
		mob.setDisplayText(previousDisplayText);
		ExternalPlay.standIfNecessary(mob);
	}



	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto)
	{
		MOB target=this.getTarget(mob,commands,givenTarget);
		if(target==null) return false;

		// if they can't hear the sleep spell, it
		// won't happen
		if((!auto)&&(!Sense.canBeHeardBy(mob,target)))
		{
			mob.tell(target.charStats().HeShe()+" can't hear your words.");
			return false;
		}


		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto))
			return false;


		boolean success=profficiencyCheck(-(target.envStats().level()*3),auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.
			invoker=mob;
			FullMsg msg=new FullMsg(mob,target,this,affectType,auto?"":"<S-NAME> incant(s) at <T-NAMESELF>.");
			if(mob.location().okAffect(msg))
			{
				mob.location().send(mob,msg);
				if(!msg.wasModified())
				{
					int a=0;
					while(a<target.numAffects())
					{
						Ability A=target.fetchAffect(a);
						int s=target.numAffects();
						if(A!=null) A.unInvoke();
						if(target.numAffects()==s)
							a++;
					}
					target.makePeace();
					ExternalPlay.standIfNecessary(target);
					previousDisplayText=((MOB)target).displayText();
					target.setDisplayText("A statue of "+target.name()+" stands here.");
					success=maliciousAffect(mob,target,mob.envStats().level()*50,-1);
					if(success)
						mob.location().show(target,null,Affect.MSG_OK_VISUAL,"<S-NAME> turn(s) into stone!!");
				}
			}
		}
		else
			return maliciousFizzle(mob,target,"<S-NAME> incant(s) at <T-NAMESELF>, but the spell fades.");

		// return whether it worked
		return success;
	}
}
