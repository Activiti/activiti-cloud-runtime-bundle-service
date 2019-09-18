/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.rest.controllers;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.spring.process.variable.DateFormatterProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskVariablesPayloadDateHandlerTest {

    private TaskVariablesPayloadDateHandler taskVariablesPayloadDateHandler = new TaskVariablesPayloadDateHandler(new DateFormatterProvider("yyyy-MM-dd[['T'][ ]HH:mm:ss[.SSS'Z']]"));

    @Test
    public void handleDatesForMap_should_convertStringToDate_whenStringRepresentsADate() {
        //given
        HashMap<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("date", "1970-01-01");
        payloadMap.put("dateTime", "1970-01-01T01:01:01.001Z");
        payloadMap.put("notADate", "this is not a date");
        payloadMap.put("int", 1);
        payloadMap.put("boolean", true);


        //calculate number of milliseconds after 1970-01-01T00:00:00.000Z
        long time = Duration.ofHours(1).toMillis() + Duration.ofMinutes(1).toMillis() + Duration.ofSeconds(1).toMillis() + 1;

        //when
        Map<String, Object> handledDates = taskVariablesPayloadDateHandler.handleDates(payloadMap);

        //then
        assertThat(handledDates).containsEntry("date", new Date(0));
        assertThat(handledDates).containsEntry("dateTime", new Date(time));
        assertThat(handledDates).containsEntry("notADate", "this is not a date");
        assertThat(handledDates).containsEntry("int", 1);
        assertThat(handledDates).containsEntry("boolean", true);
    }

    @Test
    public void handleDateForCreateTaskVariablePayload_should_convertStringToDate_when_stringRepresentsADate() {
        //given
        CreateTaskVariablePayload payload = TaskPayloadBuilder.createVariable()
                .withVariable("date",
                              "1970-01-01")
                .build();

        //when
        CreateTaskVariablePayload handledVariablePayload = taskVariablesPayloadDateHandler.handleDate(payload);

        //then
        assertThat(handledVariablePayload.getValue()).isEqualTo(new Date(0));
    }

    @Test
    public void handleDateForCreateTaskVariablePayload_should_convertStringToDate_when_stringRepresentsADateWithTime() {
        //given
        CreateTaskVariablePayload payload = TaskPayloadBuilder.createVariable()
                .withVariable("date",
                              "1970-01-01T01:01:01.001Z")
                .build();
        //calculate number of milliseconds after 1970-01-01T00:00:00.000Z
        long time = Duration.ofHours(1).toMillis() + Duration.ofMinutes(1).toMillis() + Duration.ofSeconds(1).toMillis() + 1;

        //when
        CreateTaskVariablePayload handledVariablePayload = taskVariablesPayloadDateHandler.handleDate(payload);

        //then
        assertThat(handledVariablePayload.getValue()).isEqualTo(new Date(time));
    }

    @Test
    public void handleDateForCreateTaskVariablePayload_should_doNothing_when_stringIsNotADate() {
        //given
        CreateTaskVariablePayload payload = TaskPayloadBuilder.createVariable()
                .withVariable("notADate",
                              "this is not a date")
                .build();

        //when
        CreateTaskVariablePayload handledVariablePayload = taskVariablesPayloadDateHandler.handleDate(payload);

        //then
        assertThat(handledVariablePayload.getValue()).isEqualTo("this is not a date");
    }

    @Test
    public void handleDateForCreateTaskVariablePayload_should_doNothing_when_itIsNotAString() {
        //given
        CreateTaskVariablePayload payload = TaskPayloadBuilder.createVariable()
                .withVariable("notAString",
                              10)
                .build();

        //when
        CreateTaskVariablePayload handledVariablePayload = taskVariablesPayloadDateHandler.handleDate(payload);

        //then
        assertThat(handledVariablePayload.getValue()).isEqualTo(10);
    }

    @Test
    public void handleDateForUpdateTaskPayload_should_convertStringToDate_when_stringRepresentsADate() {
        //given
        UpdateTaskVariablePayload updateTaskVariablePayload = TaskPayloadBuilder.updateVariable().withVariable("date",
                                                                                          "1970-01-01").build();
        //when
        UpdateTaskVariablePayload handledDatePayload = taskVariablesPayloadDateHandler.handleDate(updateTaskVariablePayload);

        //then
        assertThat(handledDatePayload.getValue()).isEqualTo(new Date(0));
    }

    @Test
    public void handleDateForUpdateTaskPayload_should_convertStringToDate_when_stringRepresentsADateWithTime() {
        //given
        UpdateTaskVariablePayload updateTaskVariablePayload = TaskPayloadBuilder
                .updateVariable()
                .withVariable("date",
                              "1970-01-01T01:01:01.001Z")
                .build();
        //calculate number of milliseconds after 1970-01-01T00:00:00.000Z
        long time = Duration.ofHours(1).toMillis() + Duration.ofMinutes(1).toMillis() + Duration.ofSeconds(1).toMillis() + 1;

        //when
        UpdateTaskVariablePayload handledDatePayload = taskVariablesPayloadDateHandler.handleDate(updateTaskVariablePayload);

        //then
        assertThat(handledDatePayload.getValue()).isEqualTo(new Date(time));
    }

    @Test
    public void handleDateForUpdateTaskPayload_should_doNothing_when_stringIsNotADate() {
        //given
        UpdateTaskVariablePayload updateTaskVariablePayload = TaskPayloadBuilder
                .updateVariable()
                .withVariable("notADate",
                              "this is not a date")
                .build();

        //when
        UpdateTaskVariablePayload handledDatePayload = taskVariablesPayloadDateHandler.handleDate(updateTaskVariablePayload);

        //then
        assertThat(handledDatePayload.getValue()).isEqualTo("this is not a date");
    }

    @Test
    public void handleDateForUpdateTaskPayload_should_doNothing_when_itIsNotAString() {
        //given
        UpdateTaskVariablePayload updateTaskVariablePayload = TaskPayloadBuilder
                .updateVariable()
                .withVariable("notAString",
                              true)
                .build();

        //when
        UpdateTaskVariablePayload handledDatePayload = taskVariablesPayloadDateHandler.handleDate(updateTaskVariablePayload);

        //then
        assertThat(handledDatePayload.getValue()).isEqualTo(true);
    }

}