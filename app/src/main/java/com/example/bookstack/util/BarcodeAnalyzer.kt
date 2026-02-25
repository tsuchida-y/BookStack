package com.example.bookstack.util

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * ML Kit を使用してカメラ画像からバーコード（ISBNコード）を検出するアナライザー。
 * CameraX の ImageAnalysis と組み合わせて使用する。
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient()


    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        //step1：画像をML Kit用に変換する
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            //解析しやすい形式（InputImage）に変換(スマホの向きも考慮)
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            //step2：バーコードスキャンを実行
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        // 本(ISBN)のバーコードは EAN_13 形式で読み取られる
                        if (barcode.format == Barcode.FORMAT_EAN_13) {
                            barcode.rawValue?.let { code ->
                                // ISBN-13形式の検証を行う
                                if (isValidISBN13(code)) {
                                    // 有効なISBN-13の場合のみコールバック関数で通知
                                    onBarcodeDetected(code)
                                }
                                // ISBN-13以外のEAN-13コード（価格コードなど）は無視
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // エラー処理（ログ出力など）
                    println("Barcode scanning failed: ${exception.message}")
                }
                .addOnCompleteListener {
                    // step3：次のフレームを処理できるようにする
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
