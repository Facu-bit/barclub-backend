package com.barclub.repository;

import com.barclub.entity.Venta;
import com.barclub.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByFecha(LocalDate fecha);

    Optional<Venta> findByPedidoId(Long pedidoId);

    List<Venta> findByMetodoPago(MetodoPago metodoPago);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha = :fecha")
    Double sumTotalByFecha(LocalDate fecha);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fecha = :fecha")
    Long countByFecha(LocalDate fecha);
}
