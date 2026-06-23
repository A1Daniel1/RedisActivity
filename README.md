# Laboratorio: Simulación de Fallos en Redis Streams (auditoria-group)

Este documento registra los comandos utilizados en `redis-cli` para implementar el tercer consumidor (`auditoria-group`) y simular la caída de un nodo antes de confirmar el procesamiento de un evento.

---

## 1. Configuración del Entorno y Grupo de Consumidores

Creación del nuevo grupo de consumidores independiente sobre el stream `banco.transferencias`:

```
XGROUP CREATE banco.transferencias auditoria-group $ MKSTREAM
```

---

## 2. Publicación de un Evento de Negocio

El productor publica una nueva transferencia en el stream:

```
XADD banco.transferencias * eventType TransferenciaCreada eventId evt-2002 transferId tr-555 amount 300000 currency COP
```

**ID generado por Redis:** `"1782233340305-0"` 

---

## 3. Consumo Inicial y Simulación de Caída

El `consumidor-auditor-1` lee el evento nuevo utilizando la opción `>`. **No se ejecuta `XACK**` para simular que el servicio sufrió un fallo inesperado justo después de recibir el payload:

```
XREADGROUP GROUP auditoria-group consumidor-auditor-1 COUNT 1 BLOCK 5000 STREAMS banco.transferencias >
```

---

## 4. Inspección de Mensajes Pendientes (PEL)

Para validar que el evento quedó atrapado en la lista de entradas pendientes (*Pending Entries List*) debido a la falta de confirmación:

```
XPENDING banco.transferencias auditoria-group
```

---

## 5. Recuperación del Evento (Tolerancia a Fallos)

Pasados los 10 segundos de inactividad, un nuevo nodo (`consumidor-auditor-2`) reclama la propiedad del mensaje huérfano para reintentar el procesamiento:

```
XCLAIM banco.transferencias auditoria-group consumidor-auditor-2 10000 1782233340305-0
```

---

## 6. Confirmación Final

Una vez que el segundo consumidor procesa el evento con éxito, envía la confirmación para sacarlo de la lista de pendientes del grupo:

```
XACK banco.transferencias auditoria-group 1782233340305-0
```
