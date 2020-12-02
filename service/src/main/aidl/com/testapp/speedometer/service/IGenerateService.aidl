// IGenerateService.aidl
package com.testapp.speedometer.service;

import com.testapp.speedometer.service.IGenerateServiceCallback;

interface IGenerateService {
    void registerCallback(IGenerateServiceCallback callbackService, double maxSpeed);
    void startGenerate();
    void stopGenerate();
    void unregisterCallback();
}
