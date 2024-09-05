package com.zscaler

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader

fun main() {
    val soFilePath =
        "../zdklibrary/app/build/intermediates/merged_native_libs/debug/out/lib/arm64-v8a/libzdkcommonlibrary.so"

    val crashAddressesInput = arrayOf(
        "00000000009a5564",
        "000000000097634c",
        "0000000000976320"
    )

    // Check if the input is valid
    if (soFilePath.isNullOrBlank()) {
        println("Invalid file path.")
        return
    }

    // Create a File object
    val soFile = File(soFilePath)

    // Check if the file exists and is a file
    if (!soFile.exists() || !soFile.isFile) {
        println("The specified path does not point to a valid .so file.")
        return
    }

    // Convert input strings to an array of shortened addresses
    val crashAddresses = crashAddressesInput.map { it.takeLast(6) }

    // Prepare the objdump command
    val command = listOf("objdump", "-d", soFile.absolutePath)

    try {
        // Create the process builder and start the process
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)  // Merge error and output streams
        val process = processBuilder.start()

        // Read the output from the command
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        // Read each line of output and append to the StringBuilder
        while (reader.readLine().also { line = it } != null) {
            output.appendLine(line)
        }

        // Wait for the process to complete
        val exitCode = process.waitFor()

        // Save the output to a file if the command executed successfully
        if (exitCode == 0) {
            // Define the output file path
            val outputFileName = "${soFile.nameWithoutExtension}_objdump_output.txt"
            val outputFile = File(soFile.parent, outputFileName)

            // Write output to the file
            BufferedWriter(FileWriter(outputFile)).use { writer ->
                writer.write(output.toString())
            }

            println("Objdump output saved to: ${outputFile.absolutePath}")

            // Print information for each crash address
            crashAddresses.forEach { crashAddress ->
                var matchFound = false
                for (line in output.lines()) {
                    if (line.contains(crashAddress)) {
                        // Find the previous line with the function name
                        val previousLineIndex = output.lines().indexOf(line) - 1
                        if (previousLineIndex >= 0) {
                            val functionLine = output.lines()[previousLineIndex]
                            println("Match found for crash address $crashAddress:")
                            println(functionLine)
                        }
                        println("Crash address found: $line")
                        matchFound = true
                    }
                }
                if (!matchFound) {
                    println("No match found for crash address: $crashAddress")
                }
            }
        } else {
            println("Error executing objdump command. Exit code: $exitCode")
        }
    } catch (e: Exception) {
        println("An error occurred while executing the objdump command: ${e.message}")
    }
}












