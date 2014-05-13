package brooklyn.entity.nosql.couchbase;

import static brooklyn.util.ssh.BashCommands.*;
import static java.lang.String.format;

import java.util.List;

import com.google.common.collect.ImmutableList;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.time.Duration;

public class CouchbaseSyncGatewaySshDriver extends AbstractSoftwareProcessSshDriver implements CouchbaseSyncGatewayDriver {
    public CouchbaseSyncGatewaySshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void stop() {

    }

    @Override
    public void install() {
        //reference http://docs.couchbase.com/sync-gateway/#getting-started-with-sync-gateway
        DownloadResolver resolver = Entities.newDownloader(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();

        OsDetails osDetails = getMachine().getMachineDetails().getOsDetails();

        if (osDetails.isLinux()) {
            List<String> commands = installLinux(urls, saveAs);
            newScript(INSTALLING)
                    .body.append(commands).execute();
        }
    }

    @Override
    public void customize() {

    }

    @Override
    public void launch() {
        ///opt/couchbase-sync-gateway/bin/sync_gateway

        Entity couchbaseNode = entity.getConfig(CouchbaseSyncGateway.COUCHBASE_NODE);
        Entities.waitForServiceUp(couchbaseNode, Duration.seconds(3 * 60));

        String serverWebAdminUrl = couchbaseNode.getAttribute(CouchbaseNode.COUCHBASE_WEB_ADMIN_URL);
        entity.setAttribute(CouchbaseSyncGateway.COUCHBASE_SERVER_WEB_URL, serverWebAdminUrl);
        newScript(LAUNCHING).body.append(format("/opt/couchbase-sync-gateway/bin/sync_gateway -url %s", serverWebAdminUrl)).execute();

    }

    private List<String> installLinux(List<String> urls, String saveAs) {

        log.info("Installing from package manager couchbase-sync-gateway version: {}", getVersion());

        String apt = chainGroup(
                "which apt-get",
                sudo("apt-get update"),
                sudo(format("dpkg -i %s", saveAs)));

        String yum = chainGroup(
                "which yum",
                sudo(format("rpm --install %s", saveAs)));

        return ImmutableList.<String>builder()
                .add(INSTALL_CURL)
                .addAll(BashCommands.commandsToDownloadUrlsAs(urls, saveAs))
                .add(alternatives(apt, yum))
                .build();
    }

    @Override
    public String getOsTag() {
        OsDetails os = getLocation().getOsDetails();
        if (os == null) {
            // Default to generic linux
            return "x86_64.rpm";
        } else {
            //FIXME should be a better way to check for OS name and version
            String osName = os.getName().toLowerCase();
            String fileExtension = osName.contains("deb") || osName.contains("ubuntu") ? ".deb" : ".rpm";
            String arch = os.is64bit() ? "x86_64" : "x86";
            return arch + fileExtension;
        }
    }

}
