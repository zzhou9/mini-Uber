package demo.rest;

import demo.model.DirectionInput;
import demo.model.GpsSimulatorRequest;
import demo.model.Point;
import demo.model.ServiceLocation;
import demo.model.SimulatorFixture;
import demo.model.VehicleStatus;
import demo.service.GpsSimulatorFactory;
import demo.service.PathService;
import demo.support.FaultCodeUtils;
import demo.task.GpsSimulator;
import demo.task.GpsSimulatorInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;


@RestController
@RequestMapping("/api")
public class LocationSimulatorRestApi {

    @Autowired
    private PathService pathService;



    @Autowired
    private GpsSimulatorFactory gpsSimulatorFactory;

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    private Map<Long, GpsSimulatorInstance> taskFutures = new HashMap<>();

    @RequestMapping("/dc")
    public List<GpsSimulatorInstance> dc(HttpServletRequest request) {
        final SimulatorFixture fixture = this.pathService.loadSimulatorFixture();

        final List<GpsSimulatorInstance> instances = new ArrayList<>();
        final List<Point> lookAtPoints = new ArrayList<>();

        final Set<Long> instanceIds = new HashSet<>(taskFutures.keySet());

        for (GpsSimulatorRequest gpsSimulatorRequest : fixture.getGpsSimulatorRequests()) {

            final GpsSimulator gpsSimulator = gpsSimulatorFactory.prepareGpsSimulator(gpsSimulatorRequest);
            lookAtPoints.add(gpsSimulator.getStartPoint());
            instanceIds.add(gpsSimulator.getId());

            final Future<?> future = taskExecutor.submit(gpsSimulator);
            final GpsSimulatorInstance instance = new GpsSimulatorInstance(gpsSimulator.getId(), gpsSimulator, future);
            taskFutures.put(gpsSimulator.getId(), instance);
            instances.add(instance);
        }



        return instances;
    }

    @RequestMapping("/status")
    public Collection<GpsSimulatorInstance> status() {
        return taskFutures.values();
    }

    @RequestMapping("/cancel")
    public int cancel() {
        int numberOfCancelledTasks = 0;
        for (Map.Entry<Long, GpsSimulatorInstance> entry : taskFutures.entrySet()) {
            GpsSimulatorInstance instance = entry.getValue();
            instance.getGpsSimulator().cancel();
            boolean wasCancelled = instance.getGpsSimulatorTask().cancel(true);
            if (wasCancelled) {
                numberOfCancelledTasks++;
            }
        }
        taskFutures.clear();

        return numberOfCancelledTasks;
    }

    @RequestMapping("/directions")
    public List<DirectionInput> directions() {
        return pathService.loadDirectionInput();
    }

    @RequestMapping("/service-locations")
    public List<ServiceLocation> serviceLocations() {
        return pathService.getServiceStations();
    }

    @RequestMapping("/fixture")
    public SimulatorFixture fixture() {

        final List<DirectionInput> directions = this.pathService.loadDirectionInput();
        final SimulatorFixture fixture = new SimulatorFixture();

        for (DirectionInput directionInput : directions) {

            final GpsSimulatorRequest gpsSimulatorRequest = new GpsSimulatorRequest();
            gpsSimulatorRequest.setExportPositionsToKml(true);
            gpsSimulatorRequest.setExportPositionsToMessaging(true);
            gpsSimulatorRequest.setMove(true);

            String polyline = this.pathService.getCoordinatesFromGoogleAsPolyline(directionInput);
            gpsSimulatorRequest.setPolyline(polyline);
            gpsSimulatorRequest.setReportInterval(1000);
            gpsSimulatorRequest.setSpeedInKph(50d);
            gpsSimulatorRequest.setExportPositionsToMessaging(true);
            gpsSimulatorRequest.setSecondsToError(60);
            gpsSimulatorRequest.setVehicleStatus(VehicleStatus.NONE);
            gpsSimulatorRequest.setFaultCode(FaultCodeUtils.getRandomFaultCode());
            fixture.getGpsSimulatorRequests().add(gpsSimulatorRequest);
        }

        return fixture;
    }



    private String getKmlUrl(HttpServletRequest request) {

        final String scheme = request.getScheme();
        final String serverName = request.getServerName();
        final int serverPort = request.getServerPort();
        final String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if ((serverPort != 80) && (serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append("/api/kml/");
        return url.toString();
    }
}
