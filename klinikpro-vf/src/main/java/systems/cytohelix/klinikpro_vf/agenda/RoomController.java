package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.RoomReq;
import systems.cytohelix.klinikpro_vf.auth.Roles;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService svc;

    public RoomController(RoomService svc) { this.svc = svc; }

    @PreAuthorize(Roles.ANY)
    @GetMapping
    public List<Room> list() { return svc.list(); }

    @PreAuthorize(Roles.ANY)
    @GetMapping("/{id}")
    public Room get(@PathVariable UUID id) { return svc.get(id); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PostMapping
    public Room create(@RequestBody RoomReq r) { return svc.create(r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @PutMapping("/{id}")
    public Room update(@PathVariable UUID id, @RequestBody RoomReq r) { return svc.update(id, r); }

    @PreAuthorize(Roles.LEADERSHIP)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { svc.delete(id); }
}
