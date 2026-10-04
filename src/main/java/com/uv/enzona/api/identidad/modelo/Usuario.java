package com.uv.enzona.api.identidad.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/**
 * Cuenta de la plataforma (tabla {@code usuario}). El CURP se guarda cifrado y nunca sale
 * en las respuestas de la API; la contraseña se guarda como hash BCrypt (RNF-01, RNF-03).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 180)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(name = "curp", length = 255)
    private String curpCifrado;

    @Column(name = "curp_hash", length = 64)
    private String curpHash;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    @Column(name = "telefono_verificado", nullable = false)
    private boolean telefonoVerificado;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private EstadoUsuario estado = EstadoUsuario.PENDIENTE;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueo_hasta")
    private OffsetDateTime bloqueoHasta;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    protected Usuario() {
    }

    public static Usuario nuevo(String nombre, String correo, String telefono, String curpCifrado,
            String curpHash, String contrasenaHash, OffsetDateTime ahora) {
        Usuario usuario = new Usuario();
        usuario.nombre = nombre;
        usuario.correo = correo;
        usuario.telefono = telefono;
        usuario.curpCifrado = curpCifrado;
        usuario.curpHash = curpHash;
        usuario.contrasenaHash = contrasenaHash;
        usuario.estado = EstadoUsuario.PENDIENTE;
        usuario.fechaRegistro = ahora;
        return usuario;
    }

    // ------------------------------------------------------------------ verificación (RF-02)

    public void verificarCorreo() {
        this.correoVerificado = true;
    }

    public void verificarTelefono() {
        this.telefonoVerificado = true;
    }

    /** Requiere el correo verificado y, si registró teléfono, también el teléfono. */
    public boolean verificacionCompleta() {
        return correoVerificado && (telefono == null || telefonoVerificado);
    }

    public void activarSiCorresponde() {
        if (estado == EstadoUsuario.PENDIENTE && verificacionCompleta()) {
            estado = EstadoUsuario.ACTIVO;
        }
    }

    // ------------------------------------------------------------------ inicio de sesión

    public boolean bloqueadoTemporalmente(OffsetDateTime ahora) {
        return bloqueoHasta != null && bloqueoHasta.isAfter(ahora);
    }

    public void registrarIntentoFallido(int maximo, int minutosBloqueo, OffsetDateTime ahora) {
        intentosFallidos++;
        if (intentosFallidos >= maximo) {
            bloqueoHasta = ahora.plusMinutes(minutosBloqueo);
            intentosFallidos = 0;
        }
    }

    public void reiniciarIntentos() {
        intentosFallidos = 0;
        bloqueoHasta = null;
    }

    // ------------------------------------------------------------------ perfil

    public void actualizarPerfil(String nombre, String telefono) {
        this.nombre = nombre;
        boolean cambioTelefono = telefono == null ? this.telefono != null : !telefono.equals(this.telefono);
        if (cambioTelefono) {
            this.telefono = telefono;
            this.telefonoVerificado = false;
        }
    }

    public void cambiarContrasena(String nuevoHash) {
        this.contrasenaHash = nuevoHash;
    }

    // ------------------------------------------------------------------ administración (RF-18)

    public void bloquear() {
        this.estado = EstadoUsuario.BLOQUEADO;
    }

    /** Reactiva la cuenta; si nunca terminó su verificación vuelve a quedar pendiente. */
    public void desbloquear() {
        this.estado = verificacionCompleta() ? EstadoUsuario.ACTIVO : EstadoUsuario.PENDIENTE;
    }

    /** Alta directa de una cuenta ya verificada (administrador inicial). */
    public void activarVerificada() {
        this.correoVerificado = true;
        this.telefonoVerificado = telefono != null;
        this.estado = EstadoUsuario.ACTIVO;
    }

    // ------------------------------------------------------------------ roles

    public void agregarRol(Rol rol) {
        roles.add(rol);
    }

    public void quitarRol(NombreRol nombreRol) {
        roles.removeIf(rol -> rol.es(nombreRol));
    }

    public boolean tieneRol(NombreRol nombreRol) {
        return roles.stream().anyMatch(rol -> rol.es(nombreRol));
    }

    public List<String> nombresRoles() {
        return roles.stream().map(Rol::getNombre).sorted(Comparator.naturalOrder()).toList();
    }

    // ------------------------------------------------------------------ lectura

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCurpCifrado() {
        return curpCifrado;
    }

    public String getCurpHash() {
        return curpHash;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public boolean isCorreoVerificado() {
        return correoVerificado;
    }

    public boolean isTelefonoVerificado() {
        return telefonoVerificado;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public Set<Rol> getRoles() {
        return Set.copyOf(roles);
    }
}
