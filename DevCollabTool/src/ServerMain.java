package dct.xmpp.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0049_privatedata.PrivateDataModule;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.server.XMPPServer;

/**
 * starts the server as a standalone application
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServerMain {

    /**
     * boots the server as a standalone application
     * 
     * adding a module from the command line:
     * using a runtime property, one or more modules can be specified, like this:
     * -Dvysper.add.module=org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PublishSubscribeModule,... more ...
     * 
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        Entity localServer = EntityImpl.parseUnchecked("vysper.org");
        Entity user1 = EntityImpl.parseUnchecked("user1@vysper.org");
        Entity user2 = EntityImpl.parseUnchecked("user2@vysper.org");
        String keystorePath;
        String keystorePassword;
        if(args.length > 2) {
            keystorePath = args[2];
            keystorePassword = args[3];
        } else {
            keystorePath = "bogus_mina_tls.cert";
            keystorePassword = "boguspw";            
        }

        
        String addedModuleProperty = System.getProperty("vysper.add.module");
        List<Module> listOfModules = null;
        if (addedModuleProperty != null) {
            String[] moduleClassNames = addedModuleProperty.split(",");
            listOfModules = createModuleInstances(moduleClassNames);
        }

        // choose the storage you want to use
        //StorageProviderRegistry providerRegistry = new JcrStorageProviderRegistry();
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        final AccountManagement accountManagement = (AccountManagement) providerRegistry
                .retrieve(AccountManagement.class);

        accountManagement.addUser(user1, "password1");
        accountManagement.addUser(user2, "password2");

        XMPPServer server = new XMPPServer(localServer.getFullQualifiedName());

        // C2S endpoint
        server.addEndpoint(new C2SEndpoint());
        
        //server.addEndpoint(new StanzaSessionFactory());
        server.setStorageProviderRegistry(providerRegistry);

        server.setTLSCertificateInfo(new File(keystorePath), keystorePassword);

        server.addModule(new SoftwareVersionModule());
        server.addModule(new EntityTimeModule());
        server.addModule(new VcardTempModule());
        server.addModule(new XmppPingModule());
        server.addModule(new PrivateDataModule());
        server.addModule(new MUCModule());
        
        if (listOfModules != null) {
            for (Module module : listOfModules) {
                server.addModule(module);
            }
        }

        server.start();
        System.out.println("vysper server is running...");

        server.getServerRuntimeContext().getServerFeatures().setRelayingToFederationServers(true);
    }

    private static List<Module> createModuleInstances(String[] moduleClassNames) {
        List<Module> modules = new ArrayList<Module>();

        for (String moduleClassName : moduleClassNames) {
            Class<Module> moduleClass;
            try {
                moduleClass = (Class<Module>) Class.forName(moduleClassName);
            } catch (ClassCastException e) {
                System.err.println("not a Vysper module class: " + moduleClassName);
                continue;
            } catch (ClassNotFoundException e) {
                System.err.println("could not load module class " + moduleClassName);
                continue;
            }
            try {
                Module module = moduleClass.newInstance();
                modules.add(module);
            } catch (Exception e) {
                System.err.println("failed to instantiate module class " + moduleClassName);
                continue;
            }
        }
        return modules;
    }
}