package com.barclub.service;

import com.barclub.dto.VentaRequestDTO;
import com.barclub.dto.VentaResponseDTO;
import com.barclub.entity.EstadoPedido;
import com.barclub.entity.Pedido;
import com.barclub.entity.Venta;
import com.barclub.exception.BusinessException;
import com.barclub.exception.ResourceNotFoundException;
import com.barclub.repository.PedidoRepository;
import com.barclub.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VentaService {

    private final VentaRepository ventaRepository;
    private final PedidoRepository pedidoRepository;

    // ---- Registrar venta (cierra el pedido) ----
    public VentaResponseDTO registrar(VentaRequestDTO dto) {
        Pedido pedido = pedidoRepository.findById(dto.getPedidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", dto.getPedidoId()));

        // Solo se puede cobrar un pedido LISTO
        if (pedido.getEstado() != EstadoPedido.LISTO) {
            throw new BusinessException(
                "Solo se pueden registrar ventas de pedidos en estado LISTO. Estado actual: " + pedido.getEstado()
            );
        }

        // Verificar que no tenga ya una venta registrada
        if (ventaRepository.findByPedidoId(dto.getPedidoId()).isPresent()) {
            throw new BusinessException("Este pedido ya tiene una venta registrada");
        }

        // Crear la venta
        Venta venta = Venta.builder()
                .fecha(LocalDate.now())
                .hora(LocalTime.now())
                .total(pedido.getTotal())
                .metodoPago(dto.getMetodoPago())
                .pedido(pedido)
                .build();

        // Marcar pedido como ENTREGADO
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedidoRepository.save(pedido);

        return toDTO(ventaRepository.save(venta));
    }

    // ---- Listar todas ----
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listarTodas() {
        return ventaRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ---- Ventas del día ----
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listarPorFecha(LocalDate fecha) {
        return ventaRepository.findByFecha(fecha)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ---- Total del día ----
    @Transactional(readOnly = true)
    public Double totalDelDia(LocalDate fecha) {
        return ventaRepository.sumTotalByFecha(fecha);
    }

    // ---- Obtener por id ----
    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerPorId(Long id) {
        return toDTO(ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", id)));
    }

    // ---- Eliminar ventas de una fecha (borrado definitivo) ----
    public void eliminarPorFecha(LocalDate fecha) {
        List<Venta> ventas = ventaRepository.findByFecha(fecha);
        ventaRepository.deleteAll(ventas);
    }

    // ---- Eliminar una venta por id ----
    public void eliminarPorId(Long id) {
        if (!ventaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venta", id);
        }
        ventaRepository.deleteById(id);
    }

    // ---- Mapper ----
    public VentaResponseDTO toDTO(Venta v) {
        String nombreCliente = null;
        String tipoPedido = null;
        if (v.getPedido() != null) {
            nombreCliente = v.getPedido().getNombreCliente() != null
                    ? v.getPedido().getNombreCliente()
                    : (v.getPedido().getCliente() != null ? v.getPedido().getCliente().getNombre() : "Sin nombre");
            tipoPedido = v.getPedido().getTipo() != null ? v.getPedido().getTipo().name() : null;
        }
        return VentaResponseDTO.builder()
                .id(v.getId())
                .fecha(v.getFecha())
                .hora(v.getHora())
                .total(v.getTotal())
                .metodoPago(v.getMetodoPago())
                .pedidoId(v.getPedido() != null ? v.getPedido().getId() : null)
                .nombreCliente(nombreCliente)
                .tipoPedido(tipoPedido)
                .build();
    }
}
