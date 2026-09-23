package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import systems.cytohelix.klinikpro_vf.agenda.AgendaDtos.RoomReq;
import systems.cytohelix.klinikpro_vf.auth.TenantContext;

@Service
public class RoomService {
    private final RoomRepository repo;

    public RoomService(RoomRepository repo) {
        this.repo = repo;
    }

    private UUID tenant() { return req(TenantContext.tenant(), "tenant"); }
    private UUID branch() { return req(TenantContext.branch(), "branch"); }
    private static UUID req(UUID v, String n) {
        if (v == null) throw new IllegalStateException("Falta " + n + " en el token");
        return v;
    }

    public List<Room> list() { return repo.findByBranchIdOrderByNameAsc(branch()); }

    public Room get(UUID id) {
        return repo.findByIdAndBranchId(id, branch())
                .orElseThrow(() -> new NoSuchElementException("Consultorio no encontrado"));
    }

    @Transactional
    public Room create(RoomReq r) {
        if (r.name() == null || r.name().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        Room room = new Room();
        room.setTenantId(tenant());
        room.setBranchId(branch());
        room.setName(r.name().trim());
        room.setLocation(r.location());
        return repo.save(room);
    }

    @Transactional
    public Room update(UUID id, RoomReq r) {
        Room room = get(id);
        if (r.name() != null && !r.name().isBlank()) room.setName(r.name().trim());
        room.setLocation(r.location());
        return repo.save(room);
    }

    @Transactional
    public void delete(UUID id) { repo.delete(get(id)); }
}
