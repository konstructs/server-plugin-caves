package org.konstructs.cave;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import akka.actor.ActorRef;
import akka.actor.Props;

import konstructs.plugin.KonstructsActor;
import konstructs.utils.*;
import konstructs.api.*;
import konstructs.api.messages.*;

public class Cave extends KonstructsActor {
    private final static LSystem SYSTEM = getLSystem();
    private final static BlockMachine MACHINE = getBlockMachine();

    private final Position position;
    private final Direction direction;
    private final BlockFactory factory;
    private final CaveConfig config;
    private final int padding;
    private int generation = 1;

    public Cave(ActorRef universe, Position position, Direction direction, BlockFactory factory,
                CaveConfig config) {
        super(universe);
        this.position = position;
        this.direction = direction;
        this.factory = factory;
        this.config = config;
        this.padding = config.getMinRadius() * 3;
        makeBoxQuery();
    }

    private Position getNonDirectionalVector() {
        if(direction.isPositive())
            return Position.ONE.subtract(direction.getVector());
        else
            return Position.ONE.add(direction.getVector());
    }

    private void makeBoxQuery() {
        int padding = (config.getMinRadius() - generation) * 4;
        int numBlocks = (int)Math.pow(2.0, (double)generation);
        Position relative = position.add(direction.getVector().multiply(numBlocks));
        int size = numBlocks + padding;
        Position nonDirectionalDimensions =
            getNonDirectionalVector()
            .multiply(size);
        Position start = relative
            .subtract(nonDirectionalDimensions);
        Position end = relative
            .add(direction.getVector().multiply(size))
            .add(nonDirectionalDimensions);
        boxShapeQuery(new InclusiveBox(start, end));
    }

    private void succeedGeneration() {
        if(generation < config.getMaxGenerations()) {
            generation++;
            makeBoxQuery();
        } else {
            growCave();
            getContext().stop(getSelf());
        }
    }

    private void failGeneration() {
        if(generation > config.getMinGenerations()) {
            generation--;
            growCave();
            getContext().stop(getSelf());
        } else {
            getContext().stop(getSelf());
        }
    }

    private void growCave() {
        String cave = "¤¤*½a";
        for(int i = 0;i < generation; i++)
            cave = SYSTEM.iterate(cave);
        replaceBlocks(CavePlugin.FILTER, MACHINE.interpret(cave, position, direction, config.getMinRadius(), 1));
    }

    @Override
    public void onBoxShapeQueryResult(BoxShapeQueryResult result) {
        for(BlockTypeId block: result.getBlocks()) {
            BlockType type = factory.getBlockType(block);
            if(!CavePlugin.FILTER.matches(block, type)) {
                failGeneration();
                return;
            }
        }
        succeedGeneration();
    }

    public static Props
        props(ActorRef universe, Position position, Direction direction, BlockFactory factory, CaveConfig config) {
        Class currentClass = new Object() { }.getClass().getEnclosingClass();

        return Props.create(currentClass, universe, position, direction, factory, config);
    }

    private static BlockMachine getBlockMachine() {
        Map<Character, BlockTypeId> blockMapping = new HashMap<Character, BlockTypeId>();
        blockMapping.put('a',BlockTypeId.VACUUM);
        blockMapping.put('b',BlockTypeId.VACUUM);
        blockMapping.put('c',BlockTypeId.VACUUM);
        blockMapping.put('d',BlockTypeId.VACUUM);
        blockMapping.put('e',BlockTypeId.VACUUM);
        return new BlockMachine(blockMapping);
    }

    private static LSystem getLSystem() {
        ProbabilisticProduction base[] = {
            new ProbabilisticProduction(15, "&b^_"),// Turn diagonal down
            new ProbabilisticProduction(15, "^c&_"),// Turn diagonal up
            new ProbabilisticProduction(15, "+d-_"),// Turn diagonal left
            new ProbabilisticProduction(15, "-e+_"),// Turn diagonal right
            new ProbabilisticProduction(5, "[%&b^_&b^_][%^c&_^c&_]"),// Split into up and down
            new ProbabilisticProduction(5, "[%+d-_+d-_][%-e+_-e+_]"),// Split into left and right
            new ProbabilisticProduction(15, "¤aa"),// Make cave bigger
            new ProbabilisticProduction(15, "%aa")// Make cave smaller
        };

        ProbabilisticProduction downwards[] = {
            new ProbabilisticProduction(80, "&b^_&b^_"),
            new ProbabilisticProduction(20, "&b^_a")
        };

        ProbabilisticProduction upwards[] = {
            new ProbabilisticProduction(80, "^c&_^c&_"),
            new ProbabilisticProduction(20, "^c&_a")
        };

        ProbabilisticProduction left[] = {
            new ProbabilisticProduction(80, "+d-_+d-_"),
            new ProbabilisticProduction(20, "+d-_a")
        };

        ProbabilisticProduction right[] = {
            new ProbabilisticProduction(80, "-e+_-e+_"),
            new ProbabilisticProduction(20, "-e+_a")
        };

        ProductionRule[] rules = {
            new ProbabilisticProductionRule("a", base),
            new ProbabilisticProductionRule("&b^_", downwards),
            new ProbabilisticProductionRule("^c&_", upwards),
            new ProbabilisticProductionRule("+d-_", left),
            new ProbabilisticProductionRule("-e+_", right)
        };

        return new LSystem(rules);
    }

}
