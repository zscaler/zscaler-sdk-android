package com.zscaler.sdk.demoapp

const val NOTIFICATION_CHANNEL_ID = "ZSCALERSDK_NOTIFICATION_ID"
const val NOTIFICATION_ID = 1
const val zpaNotConnectedHtml = """
    <html>
        <head>
            <style>
                body {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="content">
                <strong>ZPA Not Connected </strong>
            </div>
        </body>
    </html>
"""

const val zpaEmptyHtml = """
    <html>
        <head>
            <style>
                body {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="content">
                <strong></strong>
            </div>
        </body>
    </html>
"""

const val zpaErrorHtml = """
    <html>
        <head>
            <style>
                body {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="content">
                <strong>An SSL error has occurred and a secure connection to the server cannot be made </strong>
            </div>
        </body>
    </html>
"""
const val apiResponseErrorHtml = """
    <html>
        <head>
            <style>
                body {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="content">
                <strong>Failed to fetch the data using retrofit client </strong>
            </div>
        </body>
    </html>
"""