package systems.cytohelix.klinikpro_vf.patients;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.*;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // Todo filtrado por sucursal (multitenant + multisucursal)
    List<Patient> findByBranchIdOrderByFullNameAsc(UUID branchId);

    Optional<Patient> findByIdAndBranchId(UUID id, UUID branchId);

    boolean existsByBranchIdAndCode(UUID branchId, String code);

    // Búsqueda por código, nombre o teléfono
    @Query("""
               select p from Patient p
               where p.branchId = :branchId and (
                  lower(p.fullName) like lower(concat('%', :q, '%'))
                  or lower(p.code)  like lower(concat('%', :q, '%'))
                  or p.phone        like concat('%', :q, '%')
               ) order by p.fullName asc
            """)
    List<Patient> search(@Param("branchId") UUID branchId, @Param("q") String q);

    // Último código numérico usado en la sucursal (para el consecutivo)
    @Query(value = "select coalesce(max((code)::int), 0) from patients where branch_id = :branchId and code ~ '^[0-9]+$'", nativeQuery = true)
    int maxCode(@Param("branchId") UUID branchId);

    // Pacientes nuevos en un rango (para el dashboard de reportes)
    @Query("select count(p) from Patient p where p.branchId = :branchId and p.createdAt >= :start and p.createdAt < :end")
    long countNewBetween(@Param("branchId") UUID branchId,
                          @Param("start") OffsetDateTime start,
                          @Param("end") OffsetDateTime end);
}
