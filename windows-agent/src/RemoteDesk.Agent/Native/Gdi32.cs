using System.Runtime.InteropServices;

namespace RemoteDesk.Agent.Native;

/// <summary>
/// Interop P/Invoke com a biblioteca Gdi32.dll do Windows para operacoes de contexto de dispositivo (DC)
/// e manipulacao de mapas de bits (DIBs/Bitmaps) em memoria.
/// </summary>
public static class Gdi32
{
    [DllImport("gdi32.dll", SetLastError = true)]
    public static extern nint CreateCompatibleDC(nint hdc);

    [DllImport("gdi32.dll", SetLastError = true)]
    public static extern nint CreateCompatibleBitmap(nint hdc, int cx, int cy);

    [DllImport("gdi32.dll", SetLastError = true)]
    public static extern nint SelectObject(nint hdc, nint hgdiobj);

    [DllImport("gdi32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool DeleteObject(nint hObject);

    [DllImport("gdi32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool DeleteDC(nint hdc);

    [DllImport("gdi32.dll", SetLastError = true)]
    public static extern int GetDIBits(
        nint hdc,
        nint hbm,
        uint start,
        uint cLines,
        byte[]? lpvBits,
        ref BITMAPINFO lpbi,
        uint usage);

    public const uint DIB_RGB_COLORS = 0;
    public const int BI_RGB = 0;

    [StructLayout(LayoutKind.Sequential)]
    public struct BITMAPINFOHEADER
    {
        public uint biSize;
        public int biWidth;
        public int biHeight;
        public ushort biPlanes;
        public ushort biBitCount;
        public uint biCompression;
        public uint biSizeImage;
        public int biXPelsPerMeter;
        public int biYPelsPerMeter;
        public uint biClrUsed;
        public uint biClrImportant;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct BITMAPINFO
    {
        public BITMAPINFOHEADER bmiHeader;
        public uint bmiColors;
    }
}
