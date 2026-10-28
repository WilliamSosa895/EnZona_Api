-- =====================================================================
--  EnZona — V6: imagen de portada del evento
--   · imagen_url: URL pública de la imagen (Cloudinary o servida por la API).
--   · imagen_id:  identificador en el almacén (public_id de Cloudinary o
--                 nombre de archivo local) para poder reemplazarla o borrarla.
--   La imagen se sube por la API (POST /api/eventos/{id}/imagen): el cliente
--   nunca recibe las credenciales del almacén.
-- =====================================================================

ALTER TABLE evento ADD COLUMN imagen_url VARCHAR(500);
ALTER TABLE evento ADD COLUMN imagen_id  VARCHAR(200);
