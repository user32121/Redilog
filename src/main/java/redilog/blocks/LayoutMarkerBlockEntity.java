package redilog.blocks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import redilog.init.RedilogBlocks;

public class LayoutMarkerBlockEntity extends BlockEntity {

    public static String CONNECTIONS_KEY = "Connections";

    public Set<BlockPos> connections = new HashSet<>();

    public LayoutMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(RedilogBlocks.MARKER_ENTITY, pos, state);
    }

    public void recalculateConnections() {
        recalculateConnections(new HashSet<>());
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        connections.clear();
        for (NbtElement connectionNbt : nbt.getList(CONNECTIONS_KEY, NbtElement.COMPOUND_TYPE)) {
            connections.add(NbtHelper.toBlockPos((NbtCompound) connectionNbt));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        NbtList list = new NbtList();
        for (BlockPos connection : connections) {
            list.add(NbtHelper.fromBlockPos(connection));
        }
        nbt.put(CONNECTIONS_KEY, list);
    }

    private void recalculateConnections(Set<BlockPos> connections) {
        this.connections = connections;

        //dfs iterate through all connections
        for (Direction dir : (Iterable<Direction>) Direction.stream()::iterator) {
            for (int i = 0; i <= 64; i++) {
                BlockPos searchPos = pos.offset(dir, i);
                if (connections.contains(searchPos)) {
                    //already visited
                    continue;
                }
                if (world.getBlockEntity(searchPos) instanceof LayoutMarkerBlockEntity lmbe) {
                    connections.add(searchPos);
                    lmbe.recalculateConnections(connections);
                }
            }
        }
        world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
    }
}
