package ttt.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ttt.common.GameServer;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;

        GameServerImpl server = new GameServerImpl();
        GameServer stub = (GameServer) UnicastRemoteObject.exportObject(server, 0);
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(GameServer.NAME, stub);

        System.out.println("Server ready on port " + port + " (Ctrl+C to stop)");
    }
}
