<template>
  <v-card>
    <v-container>
      <v-row v-if="message">
        <v-col cols="12">
          <v-card class="elevation-5">
            <v-alert
              dense
              outlined
              dismissible
              transition="scale-transition"
              :type="isError ? 'error' : 'success'"
            >
              {{ message }}
            </v-alert>
          </v-card>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-form @submit.prevent="findOrder" v-model="valid" ref="searchForm">
            <v-row>
              <v-col cols="12" class="pb-0 mb-0">
                <v-radio-group v-model="orderType" row>
                  <v-radio label="订单" value="order" color="green"></v-radio>
                  <v-radio label="用户" value="user" color="green"></v-radio>
                  <v-radio label="图书" value="book" color="green"></v-radio>
                </v-radio-group>
              </v-col>
              <v-col cols="12" sm="4" class="pt-0 mt-0">
                <v-text-field
                  :label="orderTypeLabel"
                  v-model="orderData"
                  :rules="[rules.required]"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="4">
                <v-btn text color="green" type="submit" dark :disabled="!valid">
                  搜索
                </v-btn>
              </v-col>
            </v-row>
          </v-form>
        </v-col>
      </v-row>
    </v-container>
    <v-data-table
      :headers="headers"
      :items="orders"
      class="elevation-20"
      item-key="id"
      :loading="loading"
      fixed-header
      height="50vh"
      :footer-props="{
        'items-per-page-text': '每页订单数'
      }"
      dense
    >
      <template v-slot:top>
        <v-dialog v-model="showReturnRecepit" overlay-opacity="0.8" persistent>
          <v-card flat v-if="returnedOrder" ref="downloadContent">
            <v-card-title class="subheading font-weight-bold green--text">
              订单 {{ returnedOrder.id }} 已成功归还！
            </v-card-title>
            <v-card-text>
              <v-list dense>
                <v-list-item>
                  <v-list-item-content>
                    逾期费用：
                  </v-list-item-content>
                  <v-list-item-content class="align-end red--text">
                    {{ returnedOrder.lateFees }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    图书 ID：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.bookId }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    用户 ID：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.userId }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    图书名称：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.bookName }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    馆藏编号：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.bookReferenceId }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    ISBN:
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.bookIsbn }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    下单时间：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.orderedAt }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    领取时间：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.collectedAt }}
                  </v-list-item-content>
                </v-list-item>
                <v-list-item>
                  <v-list-item-content>
                    归还时间：
                  </v-list-item-content>
                  <v-list-item-content class="align-end">
                    {{ returnedOrder.returnedAt }}
                  </v-list-item-content>
                </v-list-item>
              </v-list>
            </v-card-text>
            <v-card-actions>
              <v-btn
                color="blue darken-1"
                text
                dark
                @click="closeReturnRecepit()"
              >
                关闭
              </v-btn>
              <v-btn
                color="green darken-1"
                text
                @click="download(returnedOrder)"
                dark
                >下载</v-btn
              >
            </v-card-actions>
          </v-card>
        </v-dialog>
      </template>
      <template v-slot:[`item.collectBy`]="{ item }">
        <span :class="getDateTextColor(item.collectBy, item.collectedAt)">
          {{ item.collectBy }}
        </span>
      </template>
      <template v-slot:[`item.returnBy`]="{ item }">
        <span :class="getDateTextColor(item.returnBy, item.returnedAt)">
          {{ item.returnBy }}
        </span>
      </template>
      <template v-slot:[`item.actions`]="{ item }">
        <v-btn
          rounded
          small
          title="领取"
          class="ma-2"
          :disabled="!!item.collectedAt"
          @click="collectOrder(item.id)"
        >
          C
        </v-btn>
        <v-btn
          rounded
          small
          title="归还"
          class="ma-2"
          @click="returnOrder(item)"
        >
          R
        </v-btn>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
import * as ruleUtil from "@/util/ruleUtil";
import * as orderService from "@/service/order";
import jsPDF from "jspdf";
export default {
  name: "FindOrders",
  data() {
    return {
      headers: [
        {
          text: "订单 ID",
          align: "start",
          value: "id",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "用户 ID",
          value: "userId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "图书 ID",
          value: "bookId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "图书名称",
          value: "bookName",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "馆藏编号",
          value: "bookReferenceId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "下单时间",
          value: "orderedAt",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "领取时间",
          value: "collectedAt",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "最晚领取",
          value: "collectBy",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "最晚归还",
          value: "returnBy",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "领取/归还",
          value: "actions",
          sortable: false,
          class: "indigo--text darken-4"
        }
      ],
      orders: [],
      loading: false,
      valid: false,
      message: null,
      isError: false,
      orderType: "user",
      orderData: 2,
      orderTypeLabel: "用户 ID",
      rules: ruleUtil.rules,
      returnedOrder: null,
      showReturnRecepit: false
    };
  },
  methods: {
    findOrder() {
      this.setMarkerParams(true, null, false);
      orderService
        .findOrders(this.orderType, this.orderData)
        .then(data => {
          if (Array.isArray(data)) {
            this.orders = data;
          } else {
            this.orders = [];
            this.orders.push(data);
          }
          this.loading = false;
        })
        .catch(err => {
          this.setMarkerParams(false, err.error_description, true);
        });
    },
    collectOrder(id) {
      this.setMarkerParams(true, null, false);
      orderService
        .collectOrder(id)
        .then(data => {
          this.findOrder();
          this.setMarkerParams(
            false,
            "订单 " + data.id + " 已标记为已领取。",
            false
          );
        })
        .catch(err => {
          this.setMarkerParams(false, err.error_description, true);
        });
    },
    returnOrder(item) {
      this.setMarkerParams(true, null, false);
      orderService
        .returnOrder(item.id)
        .then(data => {
          if (this.orderType === "order") {
            this.orders = [];
          } else {
            this.findOrder();
          }
          this.setMarkerParams(false, "订单已标记为已归还。", false);
          this.displayReturnReceipt(data);
        })
        .catch(err => {
          this.setMarkerParams(false, err.error_description, true);
        });
    },
    getDateTextColor(affectedDate, dependentDate) {
      let plainColor = this.$vuetify.theme.isDark
        ? "white--text"
        : "black--text";
      if (dependentDate != null) {
        return plainColor;
      }
      let diff = this.getDayDifferenceFromNow(affectedDate);

      if (diff <= 0) {
        return this.$vuetify.theme.isDark
          ? "red--text darken-4"
          : "red--text darken-4";
      } else if (diff <= 3) {
        return this.$vuetify.theme.isDark
          ? "yellow--text lighten-1"
          : "orange--text lighten-2";
      } else {
        return plainColor;
      }
    },
    getDayDifferenceFromNow(date) {
      let time = Date.parse(date);
      let now = Date.now();
      let diff = time - now;
      return diff / (1000 * 3600 * 24);
    },
    setMarkerParams(loading, message, isError) {
      this.loading = loading;
      this.message = message;
      this.isError = isError;
    },
    displayReturnReceipt(item) {
      this.showReturnRecepit = true;
      this.returnedOrder = item;
    },
    closeReturnRecepit() {
      this.showReturnRecepit = false;
      this.returnedOrder = null;
    },
    download(order) {
      const doc = new jsPDF();
      doc.text("LMS - 图书管理系统", 10, 10);
      doc.text("用户 ID: " + order.userId, 10, 20);
      doc.text("订单 ID: " + order.id, 10, 30);
      doc.text("逾期费用: " + order.lateFees, 10, 40);
      doc.text("图书名称: " + order.bookName, 10, 50);
      doc.text("馆藏编号: " + order.bookReferenceId, 10, 60);
      doc.text("ISBN : " + order.bookIsbn, 10, 70);
      doc.text("下单时间: " + order.orderedAt, 10, 80);
      doc.text("领取时间: " + order.collectedAt, 10, 90);
      doc.text("归还时间: " + order.returnedAt, 10, 100);
      doc.save(order.id + "_recepit.pdf");
    }
  },
  watch: {
    orderType(newValue) {
      switch (newValue) {
        case "order":
          this.orderTypeLabel = "订单 ID";
          break;
        case "user":
          this.orderTypeLabel = "用户 ID";
          break;
        case "book":
          this.orderTypeLabel = "图书 ID";
          break;
      }
    }
  },
  mounted() {
    this.findOrder();
  }
};
</script>

<style></style>
