<template>
  <v-card class="mb-12" :loading="loading">
    <v-card-text v-if="selectedNewBook">
      <v-list>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>
              正在查询馆藏：
              <span class="green--text">{{ selectedNewBook.name }}</span>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>
              <p v-if="count > 0">
                剩余可借数量：<span class="green--text">{{ count }}</span>
              </p>
              <p v-else>当前没有可借馆藏。</p>
              <v-btn class="pa-0" text @click="searchInventory">
                <v-icon>mdi-refresh</v-icon> 刷新
              </v-btn>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-card-text>
    <v-card-actions>
      <v-btn color="green" text :disabled="count <= 0" @click="finalizeOrder">
        继续
      </v-btn>

      <v-btn text @click="goBack">
        返回
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import * as inventoryService from "@/service/inventory";
export default {
  name: "SearchInventory",
  data() {
    return {
      count: 0,
      loading: false
    };
  },
  props: {
    selectedNewBook: {
      type: Object,
      default: null
    }
  },
  methods: {
    finalizeOrder() {
      this.$emit("finalizeOrder");
    },
    goBack() {
      this.$emit("goBack");
    },
    searchInventory() {
      this.loading = true;
      inventoryService
        .findAvailableBookCount(this.selectedNewBook.id)
        .then(data => {
          this.count = data;
          this.loading = false;
          console.log("Search Inventory for", this.count);
        })
        .catch(err => {
          console.log(err);
          this.loading = false;
          this.count = 0;
        });
    }
  },
  watch: {
    selectedNewBook: {
      handler() {
        if (this.selectedNewBook) {
          this.searchInventory();
        }
      }
    }
  }
};
</script>

<style></style>
