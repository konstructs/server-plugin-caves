package org.konstructs.cave;

import java.util.Random;
import java.util.Map;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;

import konstructs.plugin.KonstructsActor;
import konstructs.plugin.PluginConstructor;
import konstructs.utils.*;
import konstructs.api.*;
import konstructs.api.messages.*;

public class CavePlugin extends KonstructsActor {
    public final static BlockFilter STONE_FILTER = BlockFilterFactory
        .withClass(BlockClassId.fromString("org/konstructs/class/Stone"));
    public final static BlockFilter FILTER =
        STONE_FILTER.or(BlockFilterFactory
            .withClass(BlockClassId.fromString("org/konstructs/class/Granular")));

    private final Random random = new Random();
    private final CaveConfig config;
    private BlockFactory factory = null;

    public CavePlugin(ActorRef universe, CaveConfig config) {
        super(universe);
        this.config = config;
        universe.tell(GetBlockFactory.MESSAGE, getSelf());
    }

    private void checkStartPosition(Position position) {
        Position start = position.add(Direction.getRandom().getVector().multiply(config.getStartPositionRadius() * 2));
        boxShapeQuery(new BoxAround(start, Position.ONE.multiply(config.getStartPositionRadius())));
    }

    @Override
    public void onBoxShapeQueryResult(BoxShapeQueryResult result) {
        for(BlockTypeId block: result.getBlocks()) {
            BlockType type = factory.getBlockType(block);
            if(!STONE_FILTER.matches(block, type)) {
                System.out.println("Failed cave start");
                return;
            }
        }
        Position start = ((BoxAround)result.getBox()).getCenter();
        getContext().actorOf(Cave.props(getUniverse(), start, Direction.getRandom(), factory, config));
    }

    @Override
    public void onBlockUpdateEvent(BlockUpdateEvent event) {
        if(factory != null) {
            for(Map.Entry<Position, BlockUpdate> p: event.getUpdatedBlocks().entrySet()) {
                BlockTypeId block = p.getValue().getBefore().getType();
                BlockType type = factory.getBlockType(block);
                if(STONE_FILTER.matches(block, type)) {
                    if(random.nextInt(10000) < config.getProbability())
                        checkStartPosition(p.getKey());
                    return;
                }
            }
        }
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof BlockFactory) {
            factory = (BlockFactory)message;
        } else {
            super.onReceive(message); // Handle konstructs messages
        }
    }

    @PluginConstructor
    public static Props
        props(
              String pluginName,
              ActorRef universe,
              Config config) {
        Class currentClass = new Object() { }.getClass().getEnclosingClass();

        return Props.create(currentClass, universe, new CaveConfig(config));
    }

}
