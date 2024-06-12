package com.example.xiaotu;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.database.Cursor;
import android.widget.ScrollView;
import android.widget.TextView;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.media.ExifInterface;
import androidx.documentfile.provider.DocumentFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import org.opencv.core.MatOfByte;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfInt;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;

public class MainActivity extends Activity
{
    // Solicitudes de archivos y directorios
    final int REQUEST_SELECT_FILES = 0;
    final int REQUEST_SELECT_DIRECTORY = 1;

    // Variables para URIs de archivos y directorio de salida
    private List<Uri> selec_uris;
    private Uri dir_salida;

    // Método de acciones para botones y eventos
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        OpenCVLoader.initDebug();

        // Visualiza el contenido de activity_main
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // IDs para botones
        Button selec_imgs = findViewById(R.id.selec_imgs);
        Button selec_dir_salida = findViewById(R.id.selec_dir_salida);
        Button redim_boton = findViewById(R.id.redim_boton);

        // Acciones para botones
        selec_imgs.setOnClickListener(x0 -> selec_arch());

        selec_dir_salida.setOnClickListener(x0 -> selec_dir());

        redim_boton.setOnClickListener(x0 ->
        {
            if (selec_uris != null && dir_salida != null)
            {
                iter_uris();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Select files and an output directory", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para eventos secundarios
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_SELECT_FILES:
                selec_uris = new ArrayList<>();

                if (data.getData() != null)
                {
                    // Obtener cantidad de un único archivo seleccionado
                    selec_uris.add(data.getData());
                }
                else if (data.getClipData() != null)
                {
                    // Obtener cantidad de multiples archivos seleccionados
                    for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    {
                        selec_uris.add(data.getClipData().getItemAt(i).getUri());
                    }
                }

                // Actualizar caja de texto de directorio de imágenes
                EditText caja_1 = findViewById(R.id.TextBox1);
                caja_1.setText(selec_uris.get(0).getPath().substring(0, selec_uris.get(0).getPath().lastIndexOf('/')));

                Toast.makeText(this, selec_uris.size() + " selected files", Toast.LENGTH_SHORT).show();
                break;

            case REQUEST_SELECT_DIRECTORY:
                dir_salida = data.getData();

                // Actualizar caja de texto de directorio de salida
                EditText caja_2 = findViewById(R.id.TextBox2);
                caja_2.setText(dir_salida.getPath());

                Toast.makeText(this, "Selected directory: " + dir_salida.getPath(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // Método para seleccionar imágenes
    private void selec_arch()
    {
        Intent inte0 = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        inte0.setType("image/*");
        inte0.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(inte0, REQUEST_SELECT_FILES);
    }

    // Método para seleccionar directorio
    private void selec_dir()
    {
        Intent inte0 = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(inte0, REQUEST_SELECT_DIRECTORY);
    }

    // Método para iterar archivos
    private void iter_uris()
    {
        for (Uri uri_buff : selec_uris)
        {
            redim_img(uri_buff, dir_salida);

            // Actualizar caja de texto de archivos procesados
            ScrollView scroll_caja0 = findViewById(R.id.scroll_caja);
            ((TextView) scroll_caja0.getChildAt(0)).append(uri_buff.getPath() + "\n");
        }

        Toast.makeText(MainActivity.this, "Finished operations", Toast.LENGTH_SHORT).show();
    }

    // Método para redimensionar imágenes
    private void redim_img(Uri uri_arch, Uri dir_salida2)
    {
        try
        {
            // Variables para almacenar nombre de archivo y directorio de salida
            String arch_nom = obt_nombre(uri_arch);

            DocumentFile dest_dir = DocumentFile.fromTreeUri(this, dir_salida2);
            DocumentFile dest_arch = dest_dir.createFile("*/*", arch_nom);

            // Leer los datos de la imagen desde el flujo de entrada y convertirlos en un arreglo de bytes
            InputStream flujo_archivo = getContentResolver().openInputStream(uri_arch);
            ByteArrayOutputStream buffer_bytes = new ByteArrayOutputStream();

            byte[] data_kbs = new byte[1024];
            int iter_kbs;

            while ((iter_kbs = flujo_archivo.read(data_kbs, 0, data_kbs.length)) != -1)
            {
                buffer_bytes.write(data_kbs, 0, iter_kbs);
            }

            byte[] buff_img = buffer_bytes.toByteArray();

            buffer_bytes.close();

            // Decodificar los datos de la imagen en una matriz de OpenCV
            Mat img_0 = Imgcodecs.imdecode(new MatOfByte(buff_img), Imgcodecs.IMREAD_UNCHANGED);

            flujo_archivo.close();

            // Si el buffer consigue almacenar la imagen
            if (!img_0.empty())
            {
                // Captura dimensiones de la imagen
                int alto_in = img_0.rows();
                int ancho_in = img_0.cols();

                // Si las dimensiones son diferentes a las de salida, se procesa la imagen
                if (!((ancho_in == 1920 && alto_in == 1080) || (ancho_in == 1080 && alto_in == 1920)))
                {
                    // Calcular nuevas dimensiones para recorte en relación 16:9 o 9:16
                    double ancho_aj = 0.0, alto_aj = 0.0, rel_lado = 0.0;

                    if (ancho_in > alto_in)
                    {
                        rel_lado = ancho_in * (9.0 / 16);
                    }
                    else
                    {
                        rel_lado = alto_in * (9.0 / 16);
                    }

                    if (ancho_in > alto_in)
                    {
                        if (rel_lado > alto_in)
                        {
                            ancho_aj = alto_in * (16.0 / 9);
                            alto_aj = alto_in;
                        }
                        else
                        {
                            ancho_aj = ancho_in;
                            alto_aj = ancho_in * (9.0 / 16);
                        }
                    }
                    else
                    {
                        if (rel_lado > ancho_in)
                        {
                            ancho_aj = ancho_in;
                            alto_aj = ancho_in * (16.0 / 9);
                        }
                        else
                        {
                            ancho_aj = alto_in * (9.0 / 16);
                            alto_aj = alto_in;
                        }
                    }

                    if (ancho_aj - (int) ancho_aj > 0)
                    {
                        ancho_aj = (int) ancho_aj + 1;
                    }
                    if (alto_aj - (int) alto_aj > 0)
                    {
                        alto_aj = (int) alto_aj + 1;
                    }

                    if (((int) ancho_in - (int) ancho_aj) % 2 != 0)
                    {
                        ancho_aj -= 1;
                    }
                    if (((int) alto_in - (int) alto_aj) % 2 != 0)
                    {
                        alto_aj -= 1;
                    }

                    int x_cent = (int) ((ancho_in - ancho_aj) / 2);
                    int y_cent = (int) ((alto_in - alto_aj) / 2);

                    // Recorte centrado
                    Mat img_rec = new Mat(img_0, new Rect(x_cent, y_cent, (int) ancho_aj, (int) alto_aj));

                    // Determinar orientación de imagen de salida
                    int ancho_fin, alto_fin;

                    if (ancho_aj > alto_aj)
                    {
                        ancho_fin = 1920;
                        alto_fin = 1080;
                    }
                    else
                    {
                        ancho_fin = 1080;
                        alto_fin = 1920;
                    }

                    // Redimensionar imagen
                    Mat img_redim = new Mat();
                    Imgproc.resize(img_rec, img_redim, new Size(ancho_fin, alto_fin));

                    // Contenedor para la imagen procesada
                    MatOfByte buffer_img = new MatOfByte();

                    // Guardar imagen procesada en buffer
                    if (arch_nom.toLowerCase().endsWith(".jpg") || arch_nom.toLowerCase().endsWith(".jpeg") || arch_nom.toLowerCase().endsWith(".jfif"))
                    {
                        // Leer los metadatos EXIF de la imagen original
                        ExifInterface img_exif = new ExifInterface(getContentResolver().openInputStream(uri_arch));

                        // Determinar orientación de la imagen
                        String exif_orientacion = img_exif.getAttribute(ExifInterface.TAG_ORIENTATION);

                        // Rotar la imagen si es necesario
                        if (exif_orientacion != null)
                        {
                            Mat imagen_rotada = img_redim.clone();

                            int int_orientacion = Integer.parseInt(exif_orientacion);

                            switch (int_orientacion)
                            {
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    Core.rotate(img_redim, imagen_rotada, Core.ROTATE_90_CLOCKWISE);
                                    break;

                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    Core.rotate(img_redim, imagen_rotada, Core.ROTATE_180);
                                    break;

                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    Core.rotate(img_redim, imagen_rotada, Core.ROTATE_90_COUNTERCLOCKWISE);
                                    break;
                            }

                            img_redim = imagen_rotada;
                        }

                        // Codificar imagen en buffer
                        Imgcodecs.imencode(".jpg", img_redim, buffer_img, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 90));
                    }
                    else if (arch_nom.toLowerCase().endsWith(".png"))
                    {
                        Imgcodecs.imencode(".png", img_redim, buffer_img, new MatOfInt(Imgcodecs.IMWRITE_PNG_COMPRESSION, 9));
                    }
                    else if (arch_nom.toLowerCase().endsWith(".bmp"))
                    {
                        Imgcodecs.imencode(".bmp", img_redim, buffer_img);
                    }
                    else if (arch_nom.toLowerCase().endsWith(".webp"))
                    {
                        Imgcodecs.imencode(".webp", img_redim, buffer_img, new MatOfInt(Imgcodecs.IMWRITE_WEBP_QUALITY, 90));
                    }

                    OutputStream img_fin = getContentResolver().openOutputStream(dest_arch.getUri());

                    if (arch_nom.toLowerCase().endsWith(".jpg") || arch_nom.toLowerCase().endsWith(".jpeg") || arch_nom.toLowerCase().endsWith(".jfif"))
                    {
                        ExifInterface img_exif = new ExifInterface(getContentResolver().openInputStream(uri_arch));

                        // Obtener fecha de imagen de origen
                        String exif_fecha = img_exif.getAttribute(ExifInterface.TAG_DATETIME);

                        if (exif_fecha != null)
                        {
                            byte[] bytes_enc = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, (byte) 0x00, (byte) 0x44, (byte) 0x45, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x00, (byte) 0x00, (byte) 0x4D, (byte) 0x4D, (byte) 0x00, (byte) 0x2A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x87, (byte) 0x69, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x00};

                            // Copiar imagen original desde índice 20 hasta final
                            byte[] buffer_img_n = buffer_img.toArray();
                            int long_buff = buffer_img_n.length;

                            byte[] bytes_buff = new byte[long_buff - 20];

                            System.arraycopy(buffer_img_n, 20, bytes_buff, 0, long_buff - 20);

                            // Calcular tamaño final de la imagen
                            int largo_fin = bytes_enc.length + bytes_buff.length;

                            byte[] bytes_fin = new byte[largo_fin];

                            // Juntar arreglos (Imagen completa con fecha en EXIF)
                            System.arraycopy(bytes_enc, 0, bytes_fin, 0, bytes_enc.length);
                            System.arraycopy(bytes_buff, 0, bytes_fin, bytes_enc.length, bytes_buff.length);

                            // Asignar variables de fecha y hora original
                            Calendar fecha_datos = Calendar.getInstance();

                            String[] partes_fecha_hora = exif_fecha.split(" ");

                            String[] partes_fecha = partes_fecha_hora[0].split(":");
                            String[] partes_hora = partes_fecha_hora[1].split(":");

                            String v_ano = partes_fecha[0];
                            String v_mes = partes_fecha[1];
                            String v_dia = partes_fecha[2];
                            String v_hora = partes_hora[0];
                            String v_minuto = partes_hora[1];
                            String v_segundo = partes_hora[2];

                            // Separar dígitos
                            char ano_d1 = v_ano.charAt(0);
                            char ano_d2 = v_ano.charAt(1);
                            char ano_d3 = v_ano.charAt(2);
                            char ano_d4 = v_ano.charAt(3);

                            char mes_d1 = v_mes.charAt(0);
                            char mes_d2 = v_mes.charAt(1);

                            char dia_d1 = v_dia.charAt(0);
                            char dia_d2 = v_dia.charAt(1);

                            char hora_d1 = v_hora.charAt(0);
                            char hora_d2 = v_hora.charAt(1);

                            char minuto_d1 = v_minuto.charAt(0);
                            char minuto_d2 = v_minuto.charAt(1);

                            char segundo_d1 = v_segundo.charAt(0);
                            char segundo_d2 = v_segundo.charAt(1);

                            // Asignar fecha en valores ASCII hexadecimales en EXIF
                            bytes_fin[52] = conv_ascii(ano_d1);
                            bytes_fin[53] = conv_ascii(ano_d2);
                            bytes_fin[54] = conv_ascii(ano_d3);
                            bytes_fin[55] = conv_ascii(ano_d4);

                            bytes_fin[57] = conv_ascii(mes_d1);
                            bytes_fin[58] = conv_ascii(mes_d2);

                            bytes_fin[60] = conv_ascii(dia_d1);
                            bytes_fin[61] = conv_ascii(dia_d2);

                            bytes_fin[63] = conv_ascii(hora_d1);
                            bytes_fin[64] = conv_ascii(hora_d2);

                            bytes_fin[66] = conv_ascii(minuto_d1);
                            bytes_fin[67] = conv_ascii(minuto_d2);

                            bytes_fin[69] = conv_ascii(segundo_d1);
                            bytes_fin[70] = conv_ascii(segundo_d2);

                            // Escribir imagen con exif en el contenedor inicial
                            buffer_img = new MatOfByte(bytes_fin);
                        }
                    }

                    // Escribir buffer como archivo
                    img_fin.write(buffer_img.toArray());

                    img_fin.close();
                }
                else
                {
                    InputStream flujo_entrada = getContentResolver().openInputStream(uri_arch);
                    OutputStream flujo_salida = getContentResolver().openOutputStream(dest_arch.getUri());

                    byte[] buffer_iter = new byte[1024];
                    int bytes_cont;

                    while ((bytes_cont = flujo_entrada.read(buffer_iter)) != -1)
                    {
                        flujo_salida.write(buffer_iter, 0, bytes_cont);
                    }

                    flujo_entrada.close();
                    flujo_salida.close();
                }
            }
        }
        catch (IOException excpt0)
        {
            Log.e("MainActivity", "Error: " + excpt0.getMessage(), excpt0);
        }
    }

    // Método para interpretar byte en ASCII
    private byte conv_ascii(char v_dig)
    {
        byte byte_tmp = 0x00;

        switch (v_dig)
        {
            case '0':
                byte_tmp = 0x30;
                break;
            case '1':
                byte_tmp = 0x31;
                break;
            case '2':
                byte_tmp = 0x32;
                break;
            case '3':
                byte_tmp = 0x33;
                break;
            case '4':
                byte_tmp = 0x34;
                break;
            case '5':
                byte_tmp = 0x35;
                break;
            case '6':
                byte_tmp = 0x36;
                break;
            case '7':
                byte_tmp = 0x37;
                break;
            case '8':
                byte_tmp = 0x38;
                break;
            case '9':
                byte_tmp = 0x39;
                break;
        }

        return byte_tmp;
    }

    // Método para obtener nombre de archivo
    private String obt_nombre(Uri uri_arch)
    {
        String buff_nombre = null;

        if (uri_arch != null)
        {
            Cursor cursor0 = getContentResolver().query(uri_arch, null, null, null, null);

            if (cursor0 != null && cursor0.moveToFirst())
            {
                int nomb_indice = cursor0.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                if (nomb_indice != -1)
                {
                    buff_nombre = cursor0.getString(nomb_indice);
                }

                cursor0.close();
            }
        }

        return buff_nombre;
    }
}